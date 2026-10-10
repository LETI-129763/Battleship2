/**
 * 
 */
package battleship;

import battleship.ui.JavaFxLauncher;

public class Main
{
	/**
	 * Main.
	 *
	 * @param args the args
	 */
	public static void main(String[] args)
    {
		if (JavaFxLauncher.relaunchIfNeeded(args)) {
			return;
		}
		System.out.println("***  Battleship  ***");

		Tasks.menu();
    }
}
