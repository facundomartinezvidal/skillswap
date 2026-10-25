import Stripe from "stripe";
import { OrderDocument } from "../models/Order";
import { CustomerDocument } from "../models/Customer";
import { calculateTotal } from "./placeOrder";

export async function chargeCustomer(
  stripe: Stripe,
  customer: CustomerDocument,
  order: OrderDocument
): Promise<void> {
  const amount = Math.round(calculateTotal(order) * 100);
  const intent = await stripe.paymentIntents.create({
    amount,
    currency: "usd",
    customer: customer.stripeCustomerId,
    confirm: true,
  });
  if (intent.status !== "succeeded") {
    throw new Error("Payment failed");
  }
  order.paymentIntentId = intent.id;
}
