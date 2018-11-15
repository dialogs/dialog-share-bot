
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import im.dlg.botsdk.Bot;
import im.dlg.botsdk.domain.InteractiveEvent;
import im.dlg.botsdk.domain.Message;
import im.dlg.botsdk.domain.Peer;
import im.dlg.botsdk.domain.User;
import im.dlg.botsdk.domain.content.DocumentContent;
import im.dlg.botsdk.domain.interactive.InteractiveAction;
import im.dlg.botsdk.domain.interactive.InteractiveButton;
import im.dlg.botsdk.domain.interactive.InteractiveGroup;
import im.dlg.botsdk.domain.media.FileLocation;

public class ShareBot {

    private static final String INITIAL_MESSAGE = "Hi! Send me content (any file, any size) or forward message to"
            + " share, to copy link or to embed.";
    private static final String CLOSE_MESSAGE = "Done! I'm happy to help!";

    private static final String TWITTER_INTENT = "https://twitter.com/intent/tweet";
    private static final String FACEBOOK_INTENT = "https://www.facebook.com/sharer/sharer.php";
    private static final String FOLLOW_LINK_TWITTER = "Clic below link to tweet \n";
    private static final String FOLLOW_LINK_FACEBOOK = "Clic below link to share to FaceBook \n";
    private static final String FOLLOW_LINK_EMAIL = "Clic below link to email a link to file \n";
    private static final String MESSAGE_LINK_EMBED = "Put this code into your web page (Modify width, height and alt text as needed) \n";
    private static final String MESSAGE_COPY_LINK = "Copy this link to share \n"; 

    private static final long TIME_OUT = 30000; // 30 * 1000 ms = 30s
    private static final long CHECK_RATE = 1000; // 1s = 1000ms

    private String shareBotName;
    private String token;
    private String shareBotTwitterAccount;
    private Bot bot;
    private Map<Integer, ShareBotStatus> mapUsersStatus;

    private enum Status {
        INIT, INIT_SENT, CONTENT_SENT, CHOOSE_SENT, SOCIAL_SENT, ENDED
    }

    private enum ContentTypes {
        TEXT, FILE, AUDIO, VIDEO, IMAGE
    }

    private static final Logger logger = Logger.getLogger(ShareBot.class.getName());

    private class ShareBotStatus {
        private Status status;
        private long lastSeen;
        private shareContent content;

        public ShareBotStatus(Status status, long lastSeen) {
            this.status = status;
            this.lastSeen = lastSeen;
        }
    }

    private abstract class shareContent {

        @SuppressWarnings("unused")
        private ContentTypes type;

        abstract public String shareToEmbed() throws InterruptedException, ExecutionException;

        abstract public String shareToLink() throws InterruptedException, ExecutionException, IOException;

        abstract public String shareToTwitter() throws InterruptedException, ExecutionException;

        abstract public String shareToFacebook() throws InterruptedException, ExecutionException;

        abstract public String shareToEmail() throws IOException, InterruptedException, ExecutionException;

    }

    private class ShareContentText extends shareContent {
        private String text;

        ShareContentText() {
            super.type = ContentTypes.TEXT;
        }

        public ShareContentText(String text) {
            this();
            this.text = text;
        }

        public String shareToEmbed() throws InterruptedException, ExecutionException {
            String url = generaTextFileOnServer(this.text);
            // String snip = "<a href=\"" + url + "\" download=\"text.txt\">" +
            // MESSAGE_DL_TEXT + "</a>";
            String snip = "<object width=\"500px\" height=\"500px\" data=\"" + url
                    + "\">Your browser does not support the object tag. </object>";
            return MESSAGE_LINK_EMBED + snip;
        }

        public String shareToLink() throws InterruptedException, ExecutionException, IOException {
            String url = generaTextFileOnServer(this.text);
            logger.info("Copy link for text generated");
            return MESSAGE_COPY_LINK +  generateTextDownloadFile(url);
        }

        public String shareToTwitter() throws InterruptedException, ExecutionException {
            String url = generaTextFileOnServer(this.text);
            logger.info("Twitter Link url generated");
            return generateTweeterUrlLink(url);
        }

        public String shareToFacebook() throws InterruptedException, ExecutionException {
            String url = generaTextFileOnServer(this.text);
            logger.info("Facebook Link share generated");
            return generateFacebookUrlLink(url);
        }

        public String shareToEmail() throws IOException, InterruptedException, ExecutionException {
            String url = generaTextFileOnServer(this.text);
            logger.info("Email file generated");
            return generateEmailFile(url);
        }
    }

    private class ShareContentDocument extends shareContent {

        private DocumentContent document;

        ShareContentDocument() {
            super.type = ContentTypes.FILE;
        }

        public ShareContentDocument(String text, DocumentContent documentContent) {
            this();
            this.document = documentContent;
        }

        public String shareToEmbed() throws InterruptedException, ExecutionException {
            // get file location and url
            FileLocation fl = new FileLocation(document.getFileId(), document.getAccessHash());
            dialog.MediaAndFilesOuterClass.FileLocation fl_outer = FileLocation.buildFileLocation(fl);
            String url = bot.mediaAndFilesApi().getFileUrl(fl_outer).get();
            // String snip = "<a href=\"" + url + "\" download=\"text.txt\">" +
            // MESSAGE_DL_TEXT + "</a>";
            String snip = "<object width=\"500px\" height=\"500px\" data=\"" + url
                    + "\">Your browser does not support the object tag. </object>";
            return MESSAGE_LINK_EMBED + snip;
        }

        public String shareToLink() throws InterruptedException, ExecutionException, IOException {
            // get file location and url
            FileLocation fl = new FileLocation(document.getFileId(), document.getAccessHash());
            dialog.MediaAndFilesOuterClass.FileLocation fl_outer = FileLocation.buildFileLocation(fl);
            String url = bot.mediaAndFilesApi().getFileUrl(fl_outer).get();
            return MESSAGE_COPY_LINK + generateHTMLForAllFile(url);
        }

        public String shareToTwitter() throws InterruptedException, ExecutionException {
            // get file location and url
            FileLocation fl = new FileLocation(document.getFileId(), document.getAccessHash());
            dialog.MediaAndFilesOuterClass.FileLocation fl_outer = FileLocation.buildFileLocation(fl);
            String url = bot.mediaAndFilesApi().getFileUrl(fl_outer).get();
            return generateTweeterUrlLink(url);
        }

        public String shareToFacebook() throws InterruptedException, ExecutionException {
            // get file location and url
            FileLocation fl = new FileLocation(document.getFileId(), document.getAccessHash());
            dialog.MediaAndFilesOuterClass.FileLocation fl_outer = FileLocation.buildFileLocation(fl);
            String url = bot.mediaAndFilesApi().getFileUrl(fl_outer).get();
            return generateFacebookUrlLink(url);
        }

        public String shareToEmail() throws InterruptedException, ExecutionException, IOException {
            // get file location and url
            FileLocation fl = new FileLocation(document.getFileId(), document.getAccessHash());
            dialog.MediaAndFilesOuterClass.FileLocation fl_outer = FileLocation.buildFileLocation(fl);
            String url = bot.mediaAndFilesApi().getFileUrl(fl_outer).get();
            return generateEmailFile(url);
        }
    }

    private ShareBotStatus getUserStatus(User user) {
        if (!mapUsersStatus.containsKey(user.getPeer().getId())) {
            mapUsersStatus.put(user.getPeer().getId(), new ShareBotStatus(Status.INIT, Instant.now().toEpochMilli()));
        }
        return mapUsersStatus.get(user.getPeer().getId());
    }

    public ShareBot(String token, String shareBotName, String shareBotTwitterAccount)
            throws InterruptedException, ExecutionException {
        this.token = token;
        this.shareBotName = shareBotName;
        this.shareBotTwitterAccount = shareBotTwitterAccount;
        this.mapUsersStatus = new HashMap<Integer, ShareBotStatus>();
        logger.info("INIT SHAREBOT " + this.token);
        this.init();
        logger.info("END INIT");
        await();
        logger.info("END AWAIT LOOP");
    }

    public ShareBot(String token, String name) throws InterruptedException, ExecutionException {
        this(token, name, "dialogsharebot");
    }

    public ShareBot(String token) throws InterruptedException, ExecutionException {
        this(token, "DIALOG SHARE BOT", "dialogsharebot");
    }

    public void await() throws InterruptedException {
        this.bot.await();
    }

    public void init() throws InterruptedException, ExecutionException {
        this.bot = Bot.start(this.token).get();
        // attach to message listener interface
        this.bot.messaging().onMessage(message -> bot.users().get(message.getPeer()).thenAccept(userOpt -> {
            userOpt.ifPresent(user -> {
                logger.info("Get user status  ...");
                ShareBotStatus userStatus = getUserStatus(user);
                switch (userStatus.status) {
                case INIT:
                    sendInitMessage(user);
                    break;
                case INIT_SENT:
                    getContentToShare(user, message);
                case CONTENT_SENT:
                    logger.info("received a message from user with CONTENT_SENT status");
                    break;
                case ENDED:
                    logger.info("received a message from user with ENDED status");
                default:
                    logger.severe("Received a message with invalid status: " + userStatus.toString());
                    break;
                }
            });
        }));

        // attach to interactive events
        this.bot.interactiveApi().onEvent(evt -> {
            handleInteractive(evt);
        });

        Timer timer = new Timer();
        // Set the schedule function
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Iterator<Map.Entry<Integer, ShareBotStatus>> it = mapUsersStatus.entrySet().iterator();
                while (it.hasNext()) {
                    Entry<Integer, ShareBotStatus> item = it.next();
                    if (item.getValue().status == Status.ENDED) {
                        logger.info("ENDED: id->" + item.getKey());
                        it.remove();
                        logger.info("-> user removed.");
                    } else if ((Instant.now().toEpochMilli() - item.getValue().lastSeen) > TIME_OUT) {
                        logger.info("TIMEOUT: id->" + item.getKey());
                        it.remove();
                        logger.info("-> user removed.");
                    }
                }
            }
        }, 0, CHECK_RATE); // 1000 Millisecond = 1 second
    }

    private void sendInitMessage(User user) {

        logger.info("sending init ...");

        bot.messaging().sendText(user.getPeer(), INITIAL_MESSAGE).thenAccept(uuid -> {
            doneInitialMesssage(uuid);
        });
        updateUserStatus(user.getPeer().getId(), Status.INIT_SENT);
        logger.info("user " + user.toString() + "status is INIT_SENT");

    }

    private void doneInitialMesssage(UUID uuid) {
        logger.info("Sent init message with uuid " + uuid);
    }

    private void getContentToShare(User user, Message message) {
        logger.info("Get user conent to share");
        String text = message.getText();
        int id = user.getPeer().getId();
        ShareBotStatus share = mapUsersStatus.get(id);

        // check type
        if (message.getMessageContent() instanceof DocumentContent) {
            logger.info(
                    "User sent document: " + ((DocumentContent) message.getMessageContent()).toString() + " to share ");
            share.content = new ShareContentDocument(text, (DocumentContent) message.getMessageContent());
        } else {
            logger.info("User sent TEXT: " + text + " to share.");
            share.content = new ShareContentText(text);
        }

        updateUserStatus(id, Status.CONTENT_SENT);

        logger.info("User status: CONTENT_SENT");
        sendChooseMethodInteractive(user).thenAccept(uuid -> {
            updateUserStatus(id, Status.CHOOSE_SENT);
            logger.info("User status: CHOOSE_SENT");
        });
    }

    private CompletableFuture<UUID> sendChooseMethodInteractive(User user) {
        // interactive group
        List<InteractiveAction> actions = new ArrayList<>();
        actions.add(new InteractiveAction("button_share_to", new InteractiveButton("button_share_to", "Share to")));
        actions.add(new InteractiveAction("button_embed", new InteractiveButton("button_embed", "Embed")));
        actions.add(new InteractiveAction("button_copy_link", new InteractiveButton("button_copy_link", "Copy Link")));

        InteractiveGroup group = new InteractiveGroup(actions);

        return bot.interactiveApi().send(user.getPeer(), group);
    }

    private CompletableFuture<UUID> sendChooseSocialMethodInteractive(Peer peer) {
        List<InteractiveAction> actions = new ArrayList<>();
        actions.add(new InteractiveAction("button_facebook", new InteractiveButton("button_facebook", "Facebook")));
        actions.add(new InteractiveAction("button_twitter", new InteractiveButton("button_twitter", "Twitter")));
        actions.add(new InteractiveAction("button_email", new InteractiveButton("button_email", "Email")));

        InteractiveGroup group = new InteractiveGroup(actions);

        return bot.interactiveApi().send(peer, group);
    }

    private void handleInteractive(InteractiveEvent evt) {

        int user_id = evt.getPeer().getId();
        Status user_status = this.mapUsersStatus.get(user_id).status;
        if (user_status != null) {
            switch (evt.getId()) {
            case "button_share_to":
                logger.info("User choosed Share To option");
                // check status and send buttons
                if (user_status == Status.CHOOSE_SENT) {
                    sendChooseSocialMethodInteractive(evt.getPeer()).thenAccept(uuid -> {
                        updateUserStatus(user_id, Status.SOCIAL_SENT);
                    });
                } else {
                    logger.severe("Received Share TO event while status isn't CHOOSE_SENT");
                    logger.severe("User status is " + user_status.toString());
                }
                break;
            case "button_embed":
                logger.info("User choosed embed option");
                // check status and send buttons
                if (user_status == Status.CHOOSE_SENT) {
                    // generate code
                    try {
                        String code = this.mapUsersStatus.get(user_id).content.shareToEmbed();
                        // send to user then mark status as ENDED
                        bot.messaging().sendText(evt.getPeer(), code).thenAccept(uuid -> {
                            this.mapUsersStatus.get(user_id).status = Status.ENDED;
                            bot.messaging().sendText(evt.getPeer(), CLOSE_MESSAGE);
                        });
                    } catch (InterruptedException | ExecutionException e) {
                        logger.severe("Error on embed");
                        e.printStackTrace();
                    }

                } else {
                    logger.severe("Received Embed event while status isn't CHOOSE_SENT");
                    logger.severe("User status is " + this.mapUsersStatus.get(user_id).status.toString());
                }
                break;

            case "button_copy_link":
                logger.info("User choosed copy link option");
                // check status and send buttons
                if (user_status == Status.CHOOSE_SENT) {
                    // generate code
                    try {
                        String code = this.mapUsersStatus.get(user_id).content.shareToLink();
                        // send to user then mark status as ENDED
                        bot.messaging().sendText(evt.getPeer(), code).thenAccept(uuid -> {
                            this.mapUsersStatus.get(user_id).status = Status.ENDED;
                            bot.messaging().sendText(evt.getPeer(), CLOSE_MESSAGE);
                        });
                    } catch (InterruptedException | ExecutionException | IOException e) {
                        logger.severe("Error on copy link");
                        e.printStackTrace();
                    }

                } else {
                    logger.severe("Received Embed event while status isn't CHOOSE_SENT");
                    logger.severe("User status is " + this.mapUsersStatus.get(user_id).status.toString());
                }
                break;

            case "button_facebook":
                // check status
                if (user_status == Status.SOCIAL_SENT) {
                    // generate code
                    try {
                        String code = this.mapUsersStatus.get(user_id).content.shareToFacebook();
                        // send to user then mark status as ENDED
                        bot.messaging().sendText(evt.getPeer(), FOLLOW_LINK_FACEBOOK + code).thenAccept(uuid -> {
                            this.mapUsersStatus.get(user_id).status = Status.ENDED;
                            bot.messaging().sendText(evt.getPeer(), CLOSE_MESSAGE);
                        });
                    } catch (InterruptedException | ExecutionException e) {
                        logger.severe("Error on Facebook share");
                        e.printStackTrace();
                    }
                } else {
                    logger.severe("Received Facebook event while status isn't SOCIAL_SENT");
                    logger.severe("User status is " + this.mapUsersStatus.get(user_id).status.toString());
                }
                break;

            case "button_twitter":
                logger.info("user choosed twitter");
                // check status
                if (user_status == Status.SOCIAL_SENT) {
                    try {
                        String code = this.mapUsersStatus.get(user_id).content.shareToTwitter();
                        // send to user then mark status as ENDED
                        bot.messaging().sendText(evt.getPeer(), FOLLOW_LINK_TWITTER + code).thenAccept(uuid -> {
                            this.mapUsersStatus.get(user_id).status = Status.ENDED;
                            bot.messaging().sendText(evt.getPeer(), CLOSE_MESSAGE);
                        });
                    } catch (InterruptedException | ExecutionException e) {
                        logger.severe("Error on Twitter share");
                        e.printStackTrace();
                    }
                } else {
                    logger.severe("Received Twitter event while status isn't SOCIAL_SENT");
                    logger.severe("User status is " + this.mapUsersStatus.get(user_id).status.toString());
                }
                break;

            case "button_email":
                // check status
                if (user_status == Status.SOCIAL_SENT) {
                    try {
                        String code = this.mapUsersStatus.get(user_id).content.shareToEmail();
                        // send to user then mark status as ENDED
                        bot.messaging().sendText(evt.getPeer(), FOLLOW_LINK_EMAIL + code).thenAccept(uuid -> {
                            this.mapUsersStatus.get(user_id).status = Status.ENDED;
                            bot.messaging().sendText(evt.getPeer(), CLOSE_MESSAGE);
                        });
                    } catch (IOException | InterruptedException | ExecutionException e) {
                        logger.severe("Error on email share");
                        e.printStackTrace();
                    }
                } else {
                    logger.severe("Received Email event while status isn't SOCIAL_SENT");
                    logger.severe("User status is " + this.mapUsersStatus.get(user_id).status.toString());
                }
                break;

            default:
                logger.info("Unknown Interactive event: " + evt.toString());
                break;
            }
        } else {
            logger.severe("user_id not in map event is: " + evt.toString());
        }

    }

    private String generaTextFileOnServer(String text) throws InterruptedException, ExecutionException {

        String fileDownloadURL = null;
        File tmpFile;

        try {
            tmpFile = File.createTempFile("sharebot-", ".txt");

            FileWriter writer = new FileWriter(tmpFile);
            writer.write(text);
            writer.close();

            fileDownloadURL = this.bot.mediaAndFilesApi().upLoadFile(tmpFile, "text/html").thenApply(fileLoc -> {
                String url = null;
                try {
                    url = this.bot.mediaAndFilesApi().getFileUrl(fileLoc).get();
                } catch (InterruptedException | ExecutionException e) {
                    logger.severe("upload error for " + fileLoc.toString());
                    e.printStackTrace();
                }
                return url;
            }).get();
            // logger.info("File link url is ->" + fileDownloadURL);
            tmpFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
            logger.severe("File IO execption");
        }

        return fileDownloadURL;
    }

    private String generateTweeterUrlLink(String url) {
        String encodedText = "";
        String encodedUrl = "";
        String encodedVia = "";
        try {
            encodedText = URLEncoder.encode("Check this out ! ", "UTF-8");
            encodedUrl = URLEncoder.encode(url, "UTF-8");
            encodedVia = URLEncoder.encode(this.shareBotTwitterAccount, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.severe("tweet text cannot be encoded.");
            e.printStackTrace();
        }

        // other possible parameters
        // see
        // https://developer.twitter.com/en/docs/twitter-for-websites/tweet-button/guides/web-intent.html
        // related=twitter%3ATwitter%20News,twitterapi%3ATwitter%20API%20News
        // in-reply-to=525001166233403393 parent tweet id

        return TWITTER_INTENT + "?url=" + encodedUrl + "&text=" + encodedText + "&via=" + encodedVia + "&hashtags="
                + "dialog" + "&";
    }

    private String generateFacebookUrlLink(String url) {
        String facebook_link;
        String encodedUrl = "";

        try {
            encodedUrl = URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.severe("facebook text cannot be encoded.");
            e.printStackTrace();
        }

        facebook_link = FACEBOOK_INTENT + "?u=" + encodedUrl + "&amp;";
        return facebook_link;
    }

    private String generateEmailFile(String text) throws IOException, InterruptedException, ExecutionException {
        File emailFile = File.createTempFile("sharebot-", ".html");
        JtwigTemplate template = JtwigTemplate.classpathTemplate("/templates/auto_email.twig");
        JtwigModel model = JtwigModel.newModel().with("language", "en").with("sharebot_name", this.shareBotName)
                .with("subject", "Link to File text").with("body", text);

        template.render(model, new FileOutputStream(emailFile));

        return bot.mediaAndFilesApi().upLoadFile(emailFile, "text/html").thenApply(fl -> {
            String str = null;
            try {
                str = bot.mediaAndFilesApi().getFileUrl(fl).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            return str;
        }).get();
    }

    private String generateTextDownloadFile(String url) throws InterruptedException, ExecutionException, IOException {
        File dlFile = File.createTempFile("sharebot-", ".html");
        JtwigTemplate template = JtwigTemplate.classpathTemplate("/templates/text_download.twig");
        JtwigModel model = JtwigModel.newModel().with("url", url).with("sharebot_name", this.shareBotName);

        template.render(model, new FileOutputStream(dlFile));

        return bot.mediaAndFilesApi().upLoadFile(dlFile, "text/html").thenApply(fl -> {
            String str = null;
            try {
                str = bot.mediaAndFilesApi().getFileUrl(fl).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            return str;
        }).get();

    }

    @SuppressWarnings("unused")
    private String generateHTMLForImageFile(String url, String filename)
            throws InterruptedException, ExecutionException, IOException {
        File imgFile = File.createTempFile("sharebot-", ".html");
        JtwigTemplate template = JtwigTemplate.classpathTemplate("/templates/html_image.twig");
        JtwigModel model = JtwigModel.newModel().with("url", url).with("filename", filename).with("sharebot_name",
                this.shareBotName);

        template.render(model, new FileOutputStream(imgFile));

        return bot.mediaAndFilesApi().upLoadFile(imgFile, "text/html").thenApply(fl -> {
            String str = null;
            try {
                str = bot.mediaAndFilesApi().getFileUrl(fl).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            return str;
        }).get();

    }

    @SuppressWarnings("unused")
    private String generateHTMLForVideoFile(String url, String mime_type)
            throws InterruptedException, ExecutionException, IOException {
        File videoFile = File.createTempFile("sharebot-", ".html");
        JtwigTemplate template = JtwigTemplate.classpathTemplate("/templates/html_video.twig");
        JtwigModel model = JtwigModel.newModel().with("url", url).with("mime_type", mime_type).with("sharebot_name",
                this.shareBotName);

        template.render(model, new FileOutputStream(videoFile));

        return bot.mediaAndFilesApi().upLoadFile(videoFile, "text/html").thenApply(fl -> {
            String str = null;
            try {
                str = bot.mediaAndFilesApi().getFileUrl(fl).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            return str;
        }).get();

    }

    @SuppressWarnings("unused")
    private String generateHTMLForAudioFile(String url, String mime_type)
            throws InterruptedException, ExecutionException, IOException {
        File audioFile = File.createTempFile("sharebot-", ".html");
        JtwigTemplate template = JtwigTemplate.classpathTemplate("/templates/html_audio.twig");
        JtwigModel model = JtwigModel.newModel().with("url", url).with("mime_type", mime_type).with("sharebot_name",
                this.shareBotName);

        template.render(model, new FileOutputStream(audioFile));

        return bot.mediaAndFilesApi().upLoadFile(audioFile, "text/html").thenApply(fl -> {
            String str = null;
            try {
                str = bot.mediaAndFilesApi().getFileUrl(fl).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            return str;
        }).get();

    }

    private String generateHTMLForAllFile(String url) throws InterruptedException, ExecutionException, IOException {
        File allFile = File.createTempFile("sharebot-", ".html");
        JtwigTemplate template = JtwigTemplate.classpathTemplate("/templates/html_all.twig");
        JtwigModel model = JtwigModel.newModel().with("url", url).with("sharebot_name", this.shareBotName);

        template.render(model, new FileOutputStream(allFile));

        return bot.mediaAndFilesApi().upLoadFile(allFile, "text/html").thenApply(fl -> {
            String str = null;
            try {
                str = bot.mediaAndFilesApi().getFileUrl(fl).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            return str;
        }).get();

    }

    @SuppressWarnings("unused")
    private static boolean checkURL(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            logger.info("user sent invalid url");
            return false;
        }
    }

    private void updateUserStatus(int id, Status newStatus) {
        ShareBotStatus shareStatus = this.mapUsersStatus.get(id);
        shareStatus.status = newStatus;
        shareStatus.lastSeen = Instant.now().toEpochMilli();
    }
}

