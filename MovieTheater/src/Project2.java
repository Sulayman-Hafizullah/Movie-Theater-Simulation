
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Project2 {
	// maximum number of customers
	public static final int NUM_CUSTOMERS = 50;
	public static final int NUM_AGENTS = 2;

	// Semaphores
	static Semaphore maxCapacity = new Semaphore(NUM_CUSTOMERS, true);// maximum number of customers let in at a time
	static Semaphore boxOffice1 = new Semaphore(0, true); // availability of the first box office
	static Semaphore boxOffice2 = new Semaphore(0, true);// availability of the second box office
	static Semaphore purchasedTicket1 = new Semaphore(0, true);// used to check if a customer finishes at the box office
																// in line 0
	static Semaphore purchasedTicket2 = new Semaphore(0, true);// used to check if a customer finishes at the box office
																// in line 1
	static Semaphore custDecision = new Semaphore(2, true);// only allows 2 customers to engage with the box office
															// agent at a time
	static Semaphore officeIDs[] = { boxOffice1, boxOffice2 };// array to store the semaphores for the two box offices
	static Semaphore lineNum[] = { purchasedTicket1, purchasedTicket2 };// array to store semaphores that check if a
																		// customer finishes at the box office
	static Semaphore seatCheck = new Semaphore(1, true);// used by box office when determining the seat availability
	static Semaphore ticketLine = new Semaphore(1, true);// lets in only one customer at a time to the ticket line
	static Semaphore ticketTear = new Semaphore(0, true);// activates the ticket tearer once there is a customer in line
	static Semaphore torn = new Semaphore(0, true);// used to indicate that the ticket taker finished the task
	static Semaphore concessionLine = new Semaphore(1, true);
	static Semaphore concessionWorker = new Semaphore(0, true);
	static Semaphore concessionPurchase = new Semaphore(0, true);

	// Globals used among the threads

	static String concessions[] = { "Popcorn", "Soda", "Popcorn and Soda" };// stores concession options
	static String movies[]; // store movie options

	static int seats[];// stores seat available for movies with the same id
	static boolean seatAvailable[] = new boolean[NUM_CUSTOMERS];// used by box office to communicate whether a seat is
																// available or not
	static String custMovie[] = new String[NUM_CUSTOMERS];// stores customer movie choice
	static int concessionChoice[] = new int[NUM_CUSTOMERS];
	static int customerID;
	static int customerIDconcession;
	static int numberofMovies = 3;
	static int line;
	static int custNums[] = new int[NUM_CUSTOMERS];

	public static void main(String[] args) throws InterruptedException, IOException {
		// reads in file and updates movie and seat list
		if (args.length < 1) {
			System.out.println("Invalid argument amount");
		} else {
			ArrayList<String> fileLines = new ArrayList<>();
			String fileName = args[0];
			File file = new File(fileName);
			@SuppressWarnings("resource")
			BufferedReader br = new BufferedReader(new FileReader(file));

			for (String content; (content = br.readLine()) != null;) {
				fileLines.add(content);
			}
			movies = new String[fileLines.size()];
			seats = new int[fileLines.size()];

			for (int i = 0; i < fileLines.size(); i++) {
				String split[] = fileLines.get(i).split("\t");
				movies[i] = split[0];
				seats[i] = Integer.parseInt(split[1]);
			}
			// stores the number of movies
			numberofMovies = fileLines.size();
			// box office threads created and started
			Thread boxAgent[] = new Thread[NUM_AGENTS];
			for (int i = 0; i < NUM_AGENTS; i++) {
				boxAgent[i] = new Thread(new BoxOfficeAgent(i));
				boxAgent[i].start();
				System.out.println("Box office agent " + i + " created");
			}
			// ticket taker thread created and started
			Thread ticketTaker = new Thread(new TicketTaker());
			ticketTaker.start();
			System.out.println("Ticket taker created");
			// concession stand thread created and started
			Thread concessions = new Thread(new ConcessionStandWorker());
			concessions.start();
			System.out.println("Concession stand worker created");

			System.out.println("Theater is open");
			// Customer threads created and started
			Thread customers[] = new Thread[NUM_CUSTOMERS];
			for (int i = 0; i < NUM_CUSTOMERS; i++) {
				customers[i] = new Thread(new Customer(i));
				customers[i].start();

			}

			// join all customer threads and stop other threads
			for (int i = 0; i < NUM_CUSTOMERS; i++) {
				customers[i].join();
				System.out.println("Joined customer " + i);
				
			}
			//print out that all customer threads were joined
			
			System.exit(0);
		}

	}

	public static int getRandomInt(int max) {
		return (int) Math.floor(Math.random() * max);
	}

	// customer threads with unique ids
	public static class Customer implements Runnable {
		// ID of the customer
		int id;

		public Customer(int id) {
			this.id = id;
		}

		@Override
		public void run() {
			try {
				int temp;
				int choiceIndex;
				maxCapacity.acquire();// only lets 50 customers be created at a time
				// System.out.println("Customer " +id+" created");
				// Thread.sleep(2000);

				custDecision.acquire();// only 2 customers can go to the box office at a time
				// customerID= id;
				// pick random movie and put in array
				choiceIndex = getRandomInt(numberofMovies);
				custMovie[id] = movies[choiceIndex];// saves customers movie choice
				System.out.println("Customer " + id + " created, buying ticket to " + custMovie[id]);

				// checks which office is open and remember choice
				// customerID= id;
				temp = line;
				if (line == 0) {
					line = 1;
				} else
					line = 0;
				// System.out.println("releasing office "+temp);
				custNums[temp] = id;
				officeIDs[temp].release();
				lineNum[temp].acquire();

				// receives variable from box office to see it seat is available
				if (seatAvailable[id] == false) {
					// customer leaves theater if there is no seats
					System.out.println(
							"the movie " + custMovie[id] + " was sold out. Customer " + id + " is now leaving.");
					custDecision.release();
					return;
				}
				// System.out.println("Customer "+id+" purchased ticket to "+custMovie[id]);
				custDecision.release();
				System.out.println("Customer " + id + " in line to see ticket taker");
				ticketLine.acquire();

				customerID = id;
				ticketTear.release();
				torn.acquire();
				ticketLine.release();
				if (getRandomInt(2) == 1)// customer has a 50% chance to visit concession stand
				{
					concessionLine.acquire();// one customer at a time
					concessionChoice[id] = getRandomInt(3);// randomly chooses concession
					System.out.println("Customer " + id + " in line to buy " + concessions[concessionChoice[id]]);
					customerIDconcession = id;
					concessionWorker.release();
					concessionPurchase.acquire();
					System.out.println("Customer " + id + " receives " + concessions[concessionChoice[id]]);
					concessionLine.release();// frees up concession line

				}
				System.out.println("Customer " + id + " enters theater to see " + custMovie[id]);
				maxCapacity.release();

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	/*
	 * Thread simulating BoxOfficeAgent
	 */
	public static class BoxOfficeAgent implements Runnable {
		int id;

		public BoxOfficeAgent(int id) {
			this.id = id;
		}

		@Override
		public void run() {

			try {
				// System.out.println("Box office agent "+id+" created");
				while (true) {
					// check which box office is available
					officeIDs[id].acquire();
					System.out.println("Box office agent " + id + " Serving customer " + custNums[id]);
					seatCheck.acquire(); // one box office at a time checks seats to prevent both selling last seat
					int seatID = 100;
					for (int i = 0; i < movies.length; i++) {
						if (movies[i] == custMovie[custNums[id]]) {
							seatID = i;// finds the seats for the user specified movie
						}
					}
					if (seats[seatID] <= 0)// checks if there are any seats available
					{
						seatAvailable[custNums[id]] = false;// notifies customer that there are no seats available
					} else {
						// decrements number of seats and indicates that a ticket was sold
						seats[seatID]--;
						seatAvailable[custNums[id]] = true;
						Thread.sleep(1500);
						System.out.println("Box office agent " + id + " Sold ticket for " + custMovie[custNums[id]]
								+ " to Customer " + custNums[id]);
					}
					seatCheck.release();// lets another box office sell a ticket
					lineNum[id].release();
				}

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	/*
	 * Thread simulating the ticket taker
	 */
	public static class TicketTaker implements Runnable {
		boolean exit = false;

		public void run() {
			try {
				while (!exit) {
					ticketTear.acquire();// waits until a customer is in line then takes their ticket
					Thread.sleep(250);
					System.out.println("Ticket taken from customer " + customerID);
					// ticketLine.release();
					torn.release();// signals that the customer can proceed
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void stopExecute() {
			exit = true;
		}
	}

	/*
	 * Thread simulating the concession stand worker
	 */
	public static class ConcessionStandWorker implements Runnable {
		@Override
		public void run() {
			try {
				while (true) {

					concessionWorker.acquire();// waits for a customer to enter the line then gives them there
												// concession of choice
					Thread.sleep(3000);
					System.out.println(concessions[concessionChoice[customerIDconcession]] + " given to customer "
							+ customerIDconcession);
					concessionPurchase.release();// signals that the purchase is complete

				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}