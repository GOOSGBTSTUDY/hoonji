package java.com.study.auction;

public class AuctionSniperEndToEndTest {

    private final FakeAuctionServer auction = new FakeAuctionServer("item-54321");
    private final ApplicationRunner application = new ApplicationRunner();

    @Test
    void sniperJoinsAuctionUntilAuctionCloses() throws XMPPException, InterruptedException {
        auction.startSellingItem();
        application.startBiddingIn(auction);
        auction.hasReceivedJoinRequestFromSniper();
        auction.announceClosed();
        application.showsSniperHasLostAuction();
    }

    @AfterEach
    void stopAuction() {
        auction.stop();
    }

    @AfterEach
    void stopApplication() {
        application.stop();
    }

    static class NotImplementedException extends UnsupportedOperationException {
        public NotImplementedException() {
        }
    }

    static class AuctionSniperDriver extends JFrameDriver {
        public AuctionSniperDriver(int timeOutMillis) {
            super(
                    new GesturePerformer(),
                    JFrameDriver.topLevelFrame(
                            named(Main.MAIN_WINDOW_NAME),
                            showingOnScreen()
                    ),
                    new AWTEventQueueProber(timeOutMillis, 100)
            );
        }

        public void showSniperStatus(String statusText) {
            new JLabelDriver(this, named(Main.SNIPER_STATUS_NAME))
                    .hasText(equalTo(statusText));
        }

        public void dispose() {
            throw new NotImplementedException();
        }
    }

    static class Main {

        public static final String MAIN_WINDOW_NAME = "main";
        public static final String SNIPER_STATUS_NAME = "sniper";

        public static void main(String xmppHostname, String sniperId, String sniperPassword, String itemId) {
            throw new NotImplementedException();
        }
    }

    class FakeAuctionServer {
        public static final String ITEM_ID_AS_LOGIN = "auction-%s";
        public static final String AUCTION_RESOURCE = "Auction";
        public static final String XMPP_HOSTNAME = "localhost";
        public static final String AUCTION_PASSWORD = "auction";

        private final String itemId;
        private final XMPPConnection connection;
        private final SingleMessageListener messageListener = new SingleMessageListener();
        private Chat currentChat;

        public FakeAuctionServer(String itemId) {
            this.itemId = itemId;
            this.connection = new XMPPConnection(XMPP_HOSTNAME);
        }

        public void startSellingItem() throws XMPPException {
            connection.connect();
            connection.login(
                    String.format(ITEM_ID_AS_LOGIN, itemId),
                    AUCTION_PASSWORD,
                    AUCTION_RESOURCE
            );
            connection.getChatManager().addChatListener(
                    (chat, createdLocally) -> {
                        currentChat = chat;
                        chat.addMessageListener(messageListener);
                    }
            );
        }

        public void hasReceivedJoinRequestFromSniper() throws InterruptedException {
            messageListener.receivesAMessage();
        }

        public void announceClosed() throws XMPPException {
            currentChat.sendMessage(new Message());
        }

        public void stop() {
            connection.disconnect();
        }

        public String getItemId() {
            return itemId;
        }

    }

    private class SingleMessageListener implements MessageListener {
        private final ArrayBlockingQueue<Message> messages =
                new ArrayBlockingQueue<>(1);

        @Override
        public void processMessage(Chat chat, Message message) {
            messages.add(message);
        }

        public void receivesAMessage() throws InterruptedException {
            assertThat("Message", messages.poll(5, TimeUnit.SECONDS), is(notNullValue()));
        }
    }

    private class ApplicationRunner {
        public static final String SNIPER_ID = "sniper";
        public static final String SNIPER_PASSWORD = "sniper";

        public static final String STATUS_JOINING = "STATUS_JOINING";
        public static final String STATUS_LOST = "STATUS_LOST";

        public AuctionSniperDriver driver;

        public void startBiddingIn(FakeAuctionServer auction) {
            Thread t = new Thread(() -> {
                try {
                    Main.main(XMPP_HOSTNAME, SNIPER_ID, SNIPER_PASSWORD, auction.getItemId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, "Test Application");

            t.setDaemon(true);
            t.start();

            driver = new AuctionSniperDriver(1000);
            driver.showSniperStatus(STATUS_JOINING);
        }

        public void showsSniperHasLostAuction() {
            driver.showSniperStatus(STATUS_LOST);
        }

        public void stop() {
            if (driver != null) {
                driver.dispose();
            }
        }
    }

}