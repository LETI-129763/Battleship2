package battleship;

public class ParameterNameDemo {
    public static int doubleValue(int validValue) {
        return validValue * 2;
    }

    public static void main(String[] args) {
        int result = doubleValue(5);
        System.out.println("Result: " + result);
    }
}
