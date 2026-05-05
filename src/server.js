const express = require("express");
const cors = require("cors");
const { randomUUID } = require("crypto");
const http = require("http");
const { WebSocketServer } = require("ws");
const { readDb, writeDb } = require("./storage");

const app = express();
const PORT = process.env.PORT || 8080;
const server = http.createServer(app);
const wss = new WebSocketServer({ server, path: "/ws" });

app.use(cors());
app.use(express.json());

function broadcastEvent(event) {
  const payload = JSON.stringify({
    timestamp: Date.now(),
    ...event
  });
  for (const client of wss.clients) {
    if (client.readyState === 1) {
      client.send(payload);
    }
  }
}

function normalizeTripPayload(payload) {
  const now = new Date().toISOString();
  return {
    id: payload.id || randomUUID(),
    ownerId: payload.ownerId || "unknown",
    title: payload.title || "",
    destination: payload.destination || "",
    startDate: payload.startDate || now,
    endDate: payload.endDate || now,
    totalBudget: Number(payload.totalBudget ?? 0),
    currencyCode: payload.currencyCode || "USD",
    coverImageUrl: payload.coverImageUrl || null,
    notes: payload.notes || ""
  };
}

function normalizePointPayload(payload, tripId) {
  const now = new Date().toISOString();
  return {
    id: payload.id || randomUUID(),
    tripId,
    name: payload.name || "",
    address: payload.address || "",
    latitude: Number(payload.latitude ?? 0),
    longitude: Number(payload.longitude ?? 0),
    arrivalDate: payload.arrivalDate || now,
    durationDays: Number(payload.durationDays ?? 1),
    estimatedCost: Number(payload.estimatedCost ?? 0),
    currencyCode: payload.currencyCode || "USD",
    isVisited: Boolean(payload.isVisited ?? false),
    category: payload.category || "OTHER",
    notes: payload.notes || ""
  };
}

function normalizeUserPayload(payload) {
  const now = Date.now();
  return {
    id: payload.id || generateNumericId(),
    displayName: payload.displayName || "",
    email: payload.email || "",
    password: payload.password || "",
    homeCity: payload.homeCity || "",
    preferredCurrency: payload.preferredCurrency || "USD",
    registeredAt: Number(payload.registeredAt ?? now)
  };
}

function generateNumericId() {
  return Date.now().toString() + Math.random().toString(36).substr(2, 9);
}

app.get("/health", (_req, res) => {
  res.json({ ok: true, service: "travel-planner-server" });
});
app.get("/", (_req, res) => {
  res.json({ message: "Travel Planner API is running" });
});
app.get("/trips", (_req, res) => {
  const db = readDb();
  res.json(db.trips);
});

app.get("/trips/:id", (req, res) => {
  const db = readDb();
  const trip = db.trips.find((t) => t.id === req.params.id);
  if (!trip) {
    res.status(404).json({ message: "Trip not found" });
    return;
  }
  res.json(trip);
});

app.post("/trips", (req, res) => {
  const db = readDb();
  const trip = normalizeTripPayload(req.body || {});
  const existingIndex = db.trips.findIndex((t) => t.id === trip.id);
  const isCreated = existingIndex === -1;
  if (isCreated) {
    db.trips.push(trip);
    if (trip.ownerId && trip.ownerId !== "unknown") {
      db.userTrips.push({ userId: trip.ownerId, tripId: trip.id });
    }
  } else {
    db.trips[existingIndex] = {
      ...db.trips[existingIndex],
      ...trip
    };
  }
  writeDb(db);
  broadcastEvent(
    isCreated
      ? {
          type: "trip",
          action: "created",
          tripId: trip.id,
          message: `Поїздку створено: ${trip.title}`
        }
      : {
          type: "trip",
          action: "updated",
          tripId: trip.id,
          message: `Поїздку оновлено: ${trip.title}`
        }
  );
  res.status(isCreated ? 201 : 200).json(trip);
});

app.put("/trips/:id", (req, res) => {
  const db = readDb();
  const index = db.trips.findIndex((t) => t.id === req.params.id);
  if (index === -1) {
    res.status(404).json({ message: "Trip not found" });
    return;
  }

  const updated = normalizeTripPayload({
    ...db.trips[index],
    ...req.body,
    id: req.params.id
  });
  db.trips[index] = updated;
  writeDb(db);
  broadcastEvent({
    type: "trip",
    action: "updated",
    tripId: updated.id,
    message: `Поїздку оновлено: ${updated.title}`
  });
  res.json(updated);
});

app.delete("/trips/:id", (req, res) => {
  const db = readDb();
  const nextTrips = db.trips.filter((t) => t.id !== req.params.id);
  const removed = nextTrips.length !== db.trips.length;
  if (!removed) {
    res.status(404).json({ message: "Trip not found" });
    return;
  }

  db.trips = nextTrips;
  db.routePoints = db.routePoints.filter((p) => p.tripId !== req.params.id);
  db.userTrips = db.userTrips.filter((link) => link.tripId !== req.params.id);
  writeDb(db);
  broadcastEvent({
    type: "trip",
    action: "deleted",
    tripId: req.params.id,
    message: "Поїздку видалено"
  });
  res.status(204).send();
});

app.get("/trips/:tripId/route-points", (req, res) => {
  const db = readDb();
  const list = db.routePoints.filter((p) => p.tripId === req.params.tripId);
  res.json(list);
});

app.get("/trips/:tripId/route-points/:pointId", (req, res) => {
  const db = readDb();
  const point = db.routePoints.find(
    (p) => p.tripId === req.params.tripId && p.id === req.params.pointId
  );
  if (!point) {
    res.status(404).json({ message: "Route point not found" });
    return;
  }
  res.json(point);
});

app.post("/trips/:tripId/route-points", (req, res) => {
  const db = readDb();
  const tripExists = db.trips.some((t) => t.id === req.params.tripId);
  if (!tripExists) {
    res.status(404).json({ message: "Trip not found" });
    return;
  }

  const point = normalizePointPayload(req.body || {}, req.params.tripId);
  db.routePoints.push(point);
  writeDb(db);
  broadcastEvent({
    type: "route_point",
    action: "created",
    tripId: req.params.tripId,
    message: `Точку маршруту додано: ${point.name}`
  });
  res.status(201).json(point);
});

app.put("/trips/:tripId/route-points/:pointId", (req, res) => {
  const db = readDb();
  const index = db.routePoints.findIndex(
    (p) => p.tripId === req.params.tripId && p.id === req.params.pointId
  );
  if (index === -1) {
    res.status(404).json({ message: "Route point not found" });
    return;
  }

  const updated = normalizePointPayload(
    {
      ...db.routePoints[index],
      ...req.body,
      id: req.params.pointId
    },
    req.params.tripId
  );

  db.routePoints[index] = updated;
  writeDb(db);
  broadcastEvent({
    type: "route_point",
    action: "updated",
    tripId: req.params.tripId,
    message: `Точку маршруту оновлено: ${updated.name}`
  });
  res.json(updated);
});

app.delete("/trips/:tripId/route-points/:pointId", (req, res) => {
  const db = readDb();
  const nextPoints = db.routePoints.filter(
    (p) => !(p.tripId === req.params.tripId && p.id === req.params.pointId)
  );
  const removed = nextPoints.length !== db.routePoints.length;
  if (!removed) {
    res.status(404).json({ message: "Route point not found" });
    return;
  }
  db.routePoints = nextPoints;
  writeDb(db);
  broadcastEvent({
    type: "route_point",
    action: "deleted",
    tripId: req.params.tripId,
    message: `Точку маршруту видалено: ${req.params.pointId}`
  });
  res.status(204).send();
});

app.get("/users", (_req, res) => {
  const db = readDb();
  res.json(db.users);
});

app.get("/users/:id", (req, res) => {
  const db = readDb();
  const user = db.users.find((u) => u.id === req.params.id);
  if (!user) {
    res.status(404).json({ message: "User not found" });
    return;
  }
  res.json(user);
});

app.post("/users", (req, res) => {
  const db = readDb();
  const email = (req.body?.email || "").trim().toLowerCase();
  if (!email) {
    res.status(400).json({ message: "Email is required" });
    return;
  }
  const existingUser = db.users.find((u) => u.email.trim().toLowerCase() === email);
  if (existingUser) {
    res.status(409).json({ message: "User already exists", user: existingUser });
    return;
  }
  const user = normalizeUserPayload(req.body || {});
  db.users.push(user);
  writeDb(db);
  broadcastEvent({
    type: "user",
    action: "created",
    userId: user.id,
    message: `Користувача створено: ${user.email}`
  });
  res.status(201).json(user);
});

app.put("/users/:id", (req, res) => {
  const db = readDb();
  const index = db.users.findIndex((u) => u.id === req.params.id);
  if (index === -1) {
    res.status(404).json({ message: "User not found" });
    return;
  }
  const updated = normalizeUserPayload({
    ...db.users[index],
    ...req.body,
    id: req.params.id
  });
  db.users[index] = updated;
  writeDb(db);
  broadcastEvent({
    type: "user",
    action: "updated",
    userId: updated.id,
    message: `Профіль оновлено: ${updated.displayName}`
  });
  res.json(updated);
});

app.delete("/users/:id", (req, res) => {
  const db = readDb();
  const nextUsers = db.users.filter((u) => u.id !== req.params.id);
  const removed = nextUsers.length !== db.users.length;
  if (!removed) {
    res.status(404).json({ message: "User not found" });
    return;
  }
  db.users = nextUsers;
  db.userTrips = db.userTrips.filter((link) => link.userId !== req.params.id);
  writeDb(db);
  broadcastEvent({
    type: "user",
    action: "deleted",
    userId: req.params.id,
    message: "Користувача видалено"
  });
  res.status(204).send();
});

app.get("/users/:userId/trips", (req, res) => {
  const db = readDb();
  const user = db.users.find((u) => u.id === req.params.userId);
  if (!user) {
    res.status(404).json({ message: "User not found" });
    return;
  }
  const tripIds = new Set(
    db.userTrips
      .filter((link) => link.userId === req.params.userId)
      .map((link) => link.tripId)
  );
  const trips = db.trips.filter((trip) => tripIds.has(trip.id));
  res.json(trips);
});

app.put("/users/:userId/trips/:tripId", (req, res) => {
  const db = readDb();
  const userExists = db.users.some((u) => u.id === req.params.userId);
  if (!userExists) {
    res.status(404).json({ message: "User not found" });
    return;
  }
  const tripExists = db.trips.some((t) => t.id === req.params.tripId);
  if (!tripExists) {
    res.status(404).json({ message: "Trip not found" });
    return;
  }
  const exists = db.userTrips.some(
    (link) => link.userId === req.params.userId && link.tripId === req.params.tripId
  );
  if (!exists) {
    db.userTrips.push({ userId: req.params.userId, tripId: req.params.tripId });
    writeDb(db);
  }
  res.status(204).send();
});

app.delete("/users/:userId/trips/:tripId", (req, res) => {
  const db = readDb();
  const nextLinks = db.userTrips.filter(
    (link) => !(link.userId === req.params.userId && link.tripId === req.params.tripId)
  );
  const removed = nextLinks.length !== db.userTrips.length;
  if (!removed) {
    res.status(404).json({ message: "User-trip relation not found" });
    return;
  }
  db.userTrips = nextLinks;
  writeDb(db);
  res.status(204).send();
});

// Invites
app.post("/trips/:tripId/invite", (req, res) => {
  const db = readDb();
  const trip = db.trips.find((t) => t.id === req.params.tripId);
  if (!trip) {
    res.status(404).json({ message: "Trip not found" });
    return;
  }
  const token = randomUUID();
  db.invites.push({
    token,
    tripId: trip.id,
    createdAt: Date.now()
  });
  writeDb(db);
  // Retrofit expects a plain string to be parsed as JSON if not using Scalars, but Gson will fail if it's not quoted.
  // Actually, Retrofit is expecting a JSON string if Gson is the only converter, so we return a quoted string or just text?
  // Let's return just the token string using express res.json (which quotes it: "uuid-...")
  res.json(token);
});

app.get("/invites/:token/preview", (req, res) => {
  const db = readDb();
  const invite = db.invites.find((i) => i.token === req.params.token);
  if (!invite) {
    res.status(404).json({ message: "Invite not found" });
    return;
  }
  const trip = db.trips.find((t) => t.id === invite.tripId);
  if (!trip) {
    res.status(404).json({ message: "Trip for this invite not found" });
    return;
  }
  
  let ownerName = "Організатор";
  if (trip.ownerId && trip.ownerId !== "unknown") {
    const owner = db.users.find(u => u.id === trip.ownerId || u.email === trip.ownerId);
    if (owner) {
      ownerName = owner.displayName || owner.email;
    }
  }

  res.json({
    token: invite.token,
    title: trip.title,
    destination: trip.destination,
    description: "Вас запросили приєднатися до маршруту, бюджету та нотаток цієї поїздки.",
    invitedBy: ownerName
  });
});

app.post("/invites/:token/accept", (req, res) => {
  const db = readDb();
  const invite = db.invites.find((i) => i.token === req.params.token);
  if (!invite) {
    res.status(404).json({ message: "Invite not found" });
    return;
  }
  const trip = db.trips.find((t) => t.id === invite.tripId);
  if (!trip) {
    res.status(404).json({ message: "Trip not found" });
    return;
  }
  // The frontend handles the user-trip linking locally and also will PUT to /users/:userId/trips/:tripId
  // This endpoint just returns the full Trip object so the frontend can save it locally.

  const userId = req.body.userId;
  if (userId) {
    const exists = db.userTrips.some(
      (link) => link.userId === userId && link.tripId === trip.id
    );
    if (!exists) {
      db.userTrips.push({ userId, tripId: trip.id });
      writeDb(db);
    }
  }

  // Відправляємо повідомлення всім підключеним клієнтам (включаючи власника)
  broadcastEvent({
    type: "invite",
    action: "accepted",
    tripId: trip.id,
    message: `Користувач приєднався до поїздки: ${trip.title}`
  });

  res.json(trip);
});

wss.on("connection", (socket) => {
  socket.send(
    JSON.stringify({
      type: "system",
      action: "connected",
      message: "WebSocket підключено",
      timestamp: Date.now()
    })
  );
});

server.listen(PORT, () => {
  console.log(`Travel Planner API is running on http://localhost:${PORT}`);
});
