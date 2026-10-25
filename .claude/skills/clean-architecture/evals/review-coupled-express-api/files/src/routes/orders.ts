import { Router } from "express";
import { placeOrder } from "../services/placeOrder";
import { OrderModel } from "../models/Order";

const router = Router();

router.post("/orders", async (req, res) => {
  try {
    const order = await placeOrder(req);
    res.status(201).json(order);
  } catch (err: any) {
    res.status(400).json({ error: err.message });
  }
});

router.get("/orders/:id", async (req, res) => {
  const order = await OrderModel.findById(req.params.id).populate("customer");
  if (!order) return res.status(404).end();
  res.json({ ...order.toObject(), total: order.total() });
});

export default router;
