---
type: llm
focus: trace
weight: 1
---

The review flags the direct use of Mongoose models (OrderModel/CustomerModel) inside the order logic and proposes a repository or gateway interface defined on the business side, with the Mongoose implementation in an outer adapter.
