import { Schema, model, Document, Types } from "mongoose";

export interface OrderItem {
  sku: string;
  qty: number;
  unitPrice: number;
}

export interface OrderDocument extends Document {
  customer: Types.ObjectId;
  items: OrderItem[];
  coupon?: string;
  discountPct?: number;
  status: "pending" | "paid" | "cancelled";
  paymentIntentId?: string;
  total(): number;
}

const OrderSchema = new Schema<OrderDocument>({
  customer: { type: Schema.Types.ObjectId, ref: "Customer", required: true },
  items: [{ sku: String, qty: Number, unitPrice: Number }],
  coupon: String,
  discountPct: Number,
  status: { type: String, enum: ["pending", "paid", "cancelled"], default: "pending" },
  paymentIntentId: String,
});

// Business rule: coupons give 15% off, otherwise use the volume discount
OrderSchema.methods.total = function (this: OrderDocument) {
  const subtotal = this.items.reduce((sum, i) => sum + i.unitPrice * i.qty, 0);
  const pct = this.coupon ? 15 : this.discountPct ?? 0;
  return subtotal * (1 - pct / 100);
};

export const OrderModel = model<OrderDocument>("Order", OrderSchema);
