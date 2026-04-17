const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.onMessageCreated = functions.firestore
    .document("threads/{threadId}/messages/{messageId}")
    .onCreate(async (snapshot, context) => {
        const message = snapshot.data();
        const threadId = context.params.threadId;

        // 1. Get thread details to find participants and title
        const threadDoc = await admin.firestore().collection("threads").document(threadId).get();
        if (!threadDoc.exists) return null;

        const threadData = threadDoc.data();
        const participants = threadData.participants || [];
        const senderId = message.senderId;

        // 2. Determine recipients (everyone except the sender)
        const recipients = participants.filter(uid => uid !== senderId);

        if (recipients.length === 0) return null;

        // 3. Notification content
        const senderName = (message.senderType === "admin") ? "Support" : (threadData.customerName || "Customer");
        const payload = {
            notification: {
                title: senderName,
                body: message.type === "text" ? message.text : `Sent a ${message.type}`,
                clickAction: "FLUTTER_NOTIFICATION_CLICK", // for flutter compatibility if needed
                sound: "default"
            },
            data: {
                threadId: threadId,
                type: "chat_message",
                senderId: senderId
            }
        };

        // 4. Send to each recipient's tokens
        const sendPromises = recipients.map(async (userId) => {
            const tokensSnapshot = await admin.firestore()
                .collection("users")
                .document(userId)
                .collection("tokens")
                .get();

            const tokens = tokensSnapshot.docs.map(doc => doc.id);

            if (tokens.length > 0) {
                return admin.messaging().sendToDevice(tokens, payload);
            }
            return null;
        });

        return Promise.all(sendPromises);
    });
