---
tags: [skill-creator]
max_turns: 20
timeout_seconds: 600
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

Tengo esta API de pedidos en Express + Mongoose y cada vez que tocamos algo se rompe otra cosa (la semana pasada cambiamos el descuento y los clientes vieron un total distinto al que les cobramos). ¿Me revisás la arquitectura y me decís qué cambiarías? El código está en src/.

Input files: src/routes/orders.ts, src/services/placeOrder.ts, src/services/billing.ts, src/models/Order.ts, src/models/Customer.ts
