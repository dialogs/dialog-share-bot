import java.util.concurrent.ExecutionException;

public class Main {

    public static void main(String[] args) {

        try {
            //TODO change values
            String shareBotName = "SHARE BOT BOB";
            String myToken = "e340f0c551bc8d2268f30e1c0f8bf8627be9997b";
            new ShareBot(myToken, shareBotName);
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }

}