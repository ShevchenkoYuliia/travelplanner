const fs = require("fs");
const path = require("path");

const DB_PATH = path.join(__dirname, "..", "data.json");

const EMPTY_DB = {
  trips: [],
  routePoints: [],
  users: [],
  userTrips: [],
  invites: []
};

function normalizeDbShape(db) {
  return {
    trips: Array.isArray(db?.trips) ? db.trips : [],
    routePoints: Array.isArray(db?.routePoints) ? db.routePoints : [],
    users: Array.isArray(db?.users) ? db.users : [],
    userTrips: Array.isArray(db?.userTrips) ? db.userTrips : [],
    invites: Array.isArray(db?.invites) ? db.invites : []
  };
}

function ensureDbFile() {
  if (!fs.existsSync(DB_PATH)) {
    fs.writeFileSync(DB_PATH, JSON.stringify(EMPTY_DB, null, 2), "utf8");
    return;
  }

  try {
    JSON.parse(fs.readFileSync(DB_PATH, "utf8"));
  } catch {
    fs.writeFileSync(DB_PATH, JSON.stringify(EMPTY_DB, null, 2), "utf8");
  }
}

function readDb() {
  ensureDbFile();
  const raw = fs.readFileSync(DB_PATH, "utf8");
  const parsed = JSON.parse(raw);
  const normalized = normalizeDbShape(parsed);
  const changed = JSON.stringify(parsed) !== JSON.stringify(normalized);
  if (changed) {
    writeDb(normalized);
  }
  return normalized;
}

function writeDb(db) {
  fs.writeFileSync(DB_PATH, JSON.stringify(db, null, 2), "utf8");
}

module.exports = {
  readDb,
  writeDb
};
