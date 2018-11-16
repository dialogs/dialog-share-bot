import java.util.concurrent.ExecutionException;

public class Main {

    public static void main(String[] args) {

        try {
            // TODO change values
            String shareBotName = "SHARE BOT BOB";
            String myToken = "xxxxxxxxxxxxxxxxxxxxxxxxxxxx";
            String shareBotTwitterAccount = "twitterAccount"; // just account name no @
            new ShareBot(myToken, shareBotName, shareBotTwitterAccount);
            // if no twitter account just:
            // new ShareBot(myToken, shareBotName);
            // if no name to share bot:
            // new ShareBot(myToken);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }

}