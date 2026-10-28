const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
const DATABASE_ID = "ai-studio-android-gptpp-2ba9327b-3c66-4efd-971f-f8a46d8a59da";

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read products", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore({ databaseId: DATABASE_ID });
  await assertFails(unauthedDb.collection("users").doc("userA").collection("products").get());
});

test("Authenticated user can read and write own products", async () => {
  const userADb = testEnv.authenticatedContext("userA").firestore({ databaseId: DATABASE_ID });
  const productRef = userADb.collection("users").doc("userA").collection("products").doc("prod1");
  
  await assertSucceeds(
    productRef.set({
      id: "prod1",
      userId: "userA",
      name: "Coffee",
      costPriceMinor: 1000,
      sellPriceMinor: 1500,
      stockQuantity: 10,
      isActive: true,
    })
  );

  await assertSucceeds(productRef.get());
});

test("UserB cannot read or write UserA products", async () => {
  const userADb = testEnv.authenticatedContext("userA").firestore({ databaseId: DATABASE_ID });
  const userBDb = testEnv.authenticatedContext("userB").firestore({ databaseId: DATABASE_ID });
  const productRefA = userADb.collection("users").doc("userA").collection("products").doc("prod1");

  await assertSucceeds(
    productRefA.set({
      id: "prod1",
      userId: "userA",
      name: "Coffee",
      costPriceMinor: 1000,
      sellPriceMinor: 1500,
    })
  );

  const productRefB = userBDb.collection("users").doc("userA").collection("products").doc("prod1");
  await assertFails(productRefB.get());
  await assertFails(productRefB.update({ sellPriceMinor: 2000 }));
});

test("User can create and read own sales", async () => {
  const userADb = testEnv.authenticatedContext("userA").firestore({ databaseId: DATABASE_ID });
  const saleRef = userADb.collection("users").doc("userA").collection("sales").doc("sale1");

  await assertSucceeds(
    saleRef.set({
      id: "sale1",
      userId: "userA",
      totalMinor: 5000,
      paymentMethod: "CASH",
      voucherNo: "TR-261027-0001",
    })
  );

  await assertSucceeds(saleRef.get());
});
