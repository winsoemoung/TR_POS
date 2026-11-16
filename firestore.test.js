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

test("Unauthenticated user cannot read or write any POS collection", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore({ databaseId: DATABASE_ID });
  await assertFails(unauthedDb.collection("users").doc("userA").collection("products").get());
  await assertFails(unauthedDb.collection("users").doc("userA").collection("sale_items").get());
  await assertFails(unauthedDb.collection("users").doc("userA").collection("receipt_templates").get());
});

test("Authenticated user can write and read full POS backup collections", async () => {
  const userADb = testEnv.authenticatedContext("userA").firestore({ databaseId: DATABASE_ID });
  const userDoc = userADb.collection("users").doc("userA");

  await assertSucceeds(
    userDoc.set({
      userId: "userA",
      email: "a@example.com",
      storeName: "san thit",
      currencySymbol: "Ks",
      taxRatePercent: 0.0,
    })
  );

  await assertSucceeds(
    userDoc.collection("products").doc("1").set({
      id: "1",
      productId: 1,
      userId: "userA",
      name: "သံချောင်း",
      costPriceMinor: 3000,
      sellPriceMinor: 4000,
      stockQuantity: 10,
      stockQuantityDecimal: 10.5,
      unitName: "ပိဿာ",
      isActive: true,
    })
  );

  await assertSucceeds(
    userDoc.collection("sales").doc("sale-uuid-1").set({
      id: "sale-uuid-1",
      userId: "userA",
      totalMinor: 2000,
      paymentMethod: "CREDIT",
      voucherNo: "TR-261116-0001",
    })
  );

  await assertSucceeds(
    userDoc.collection("sale_items").doc("sale-uuid-1_1").set({
      id: "sale-uuid-1_1",
      userId: "userA",
      lineId: 1,
      saleId: "sale-uuid-1",
      productId: 1,
      productNameSnapshot: "သံချောင်း",
      unitPriceMinor: 4000,
      quantity: 1,
      lineTotalMinor: 2000,
      quantityDecimal: 0.5,
    })
  );

  await assertSucceeds(
    userDoc.collection("payments").doc("pay-1").set({
      id: "pay-1",
      userId: "userA",
      saleId: "sale-uuid-1",
      method: "CREDIT",
      tenderedMinor: 0,
    })
  );

  await assertSucceeds(
    userDoc.collection("debt_transactions").doc("10").set({
      id: "10",
      txId: 10,
      userId: "userA",
      customerId: 5,
      saleId: "sale-uuid-1",
      type: "CREDIT_SALE",
      amountMinor: 2000,
      remainingBalanceMinor: 2000,
    })
  );

  await assertSucceeds(
    userDoc.collection("suppliers").doc("1").set({
      id: "1",
      supplierId: 1,
      userId: "userA",
      name: "Shwe Supplier",
    })
  );

  await assertSucceeds(
    userDoc.collection("purchases").doc("1").set({
      id: "1",
      purchaseId: 1,
      userId: "userA",
      productId: 1,
      productName: "သံချောင်း",
      quantity: 20,
      unitCostMinor: 3000,
      totalCostMinor: 60000,
    })
  );

  await assertSucceeds(
    userDoc.collection("return_transactions").doc("ret-1").set({
      id: "ret-1",
      userId: "userA",
      saleId: "sale-uuid-1",
      returnedAmountMinor: 2000,
    })
  );

  await assertSucceeds(
    userDoc.collection("shifts").doc("1").set({
      id: "1",
      shiftId: 1,
      userId: "userA",
      cashierName: "Admin",
      openTimeEpochMs: 1700000000000,
    })
  );

  await assertSucceeds(
    userDoc.collection("stock_movements").doc("mov-1").set({
      id: "mov-1",
      userId: "userA",
      productId: 1,
      movementType: "PURCHASE",
      quantityChange: 20,
    })
  );

  await assertSucceeds(
    userDoc.collection("receipt_templates").doc("1").set({
      id: "1",
      templateId: 1,
      userId: "userA",
      name: "Template 1",
      paperWidthDots: 576,
      itemNameWidthWeight: 0.42,
    })
  );
});

test("Cross-user isolation blocks UserB from reading or writing UserA collections", async () => {
  const userADb = testEnv.authenticatedContext("userA").firestore({ databaseId: DATABASE_ID });
  const userBDb = testEnv.authenticatedContext("userB").firestore({ databaseId: DATABASE_ID });

  await assertSucceeds(
    userADb.collection("users").doc("userA").collection("sale_items").doc("s1_1").set({
      id: "s1_1",
      userId: "userA",
      saleId: "s1",
      productId: 1,
      productNameSnapshot: "Cement",
      unitPriceMinor: 10000,
      quantity: 1,
      lineTotalMinor: 10000,
    })
  );

  await assertFails(userBDb.collection("users").doc("userA").collection("sale_items").get());
  await assertFails(
    userBDb.collection("users").doc("userA").collection("sale_items").doc("s1_2").set({
      id: "s1_2",
      userId: "userB",
      saleId: "s1",
      productId: 1,
      productNameSnapshot: "Cement",
      unitPriceMinor: 10000,
      quantity: 1,
      lineTotalMinor: 10000,
    })
  );
});
