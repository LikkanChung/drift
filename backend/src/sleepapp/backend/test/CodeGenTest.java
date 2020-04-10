package sleepapp.backend.test;

import sleepapp.backend.endpoint.PairEndpoint;

public class CodeGenTest {

    //Note: change the visibility of genCode to public
    //TODO change it back?
    public static void main(String[] args) {
        PairEndpoint endpoint = new PairEndpoint(null);
        for (int i = 0; i < 50; i++) {
            System.out.println(endpoint.genCode(4));
        }
    }

}
