---
type: llm
focus: trace
weight: 1
---

The review detects the circular import between placeOrder.ts and billing.ts (calculateTotal / chargeCustomer) and proposes a way to break it.
