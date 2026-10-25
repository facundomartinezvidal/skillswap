import { Schema, model, Document } from "mongoose";

export interface CustomerDocument extends Document {
  name: string;
  email: string;
  status: "active" | "suspended";
  stripeCustomerId: string;
}

const CustomerSchema = new Schema<CustomerDocument>({
  name: String,
  email: String,
  status: { type: String, enum: ["active", "suspended"], default: "active" },
  stripeCustomerId: String,
});

export const CustomerModel = model<CustomerDocument>("Customer", CustomerSchema);
