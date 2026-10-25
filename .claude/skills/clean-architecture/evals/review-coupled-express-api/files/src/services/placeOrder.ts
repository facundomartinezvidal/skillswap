import { Request } from "express";
import Stripe from "stripe";
import sgMail from "@sendgrid/mail";
import { OrderModel, OrderDocument } from "../models/Order";
import { CustomerModel } from "../models/Customer";
import { chargeCustomer } from "./billing";

const stripe = new Stripe(process.env.STRIPE_KEY!, { apiVersion: "2024-06-20" });
sgMail.setApiKey(process.env.SENDGRID_KEY!);

export async function placeOrder(req: Request): Promise<OrderDocument> {
  const { customerId, items, coupon } = req.body;

  const customer = await CustomerModel.findById(customerId);
  if (!customer) throw new Error("Customer not found");
  if (customer.status === "suspended") throw new Error("Account suspended");

  const order = new OrderModel({ customer: customer._id, items, coupon, status: "pending" });

  // 10% off for orders with more than 3 items, unless a coupon is used
  if (!coupon && items.length > 3) {
    order.discountPct = 10;
  }

  await order.save();

  await chargeCustomer(stripe, customer, order);

  order.status = "paid";
  await order.save();

  await sgMail.send({
    to: customer.email,
    from: "orders@shop.example",
    subject: `Order ${order._id} confirmed`,
    html: `<p>Thanks! Your total is $${order.total().toFixed(2)}</p>`,
  });

  return order;
}

export function calculateTotal(order: OrderDocument): number {
  const subtotal = order.items.reduce((sum, i) => sum + i.unitPrice * i.qty, 0);
  return subtotal * (1 - (order.discountPct ?? 0) / 100);
}
