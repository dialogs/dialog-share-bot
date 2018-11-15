import java.util.concurrent.ExecutionException;

public class Main {

    public static void main(String[] args) {

        try {
            //TODO change values
            String shareBotName = "SHARE BOT BOB";
            String myToken = "xxxxxxxxxxxxxxxxxxxxxxxxxxxx";
            new ShareBot(myToken, shareBotName);
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }

}