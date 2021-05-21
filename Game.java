package game;

//By Donovan Webb - 1933061

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

import java.util.*;
import java.util.List;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.swing.*;

import javax.imageio.ImageIO;

import javax.sound.midi.*;


public class Game {
    private boolean reverse;
    public Deck deck = new Deck();
    public Player players[] = new Player[9];
	public Gui gui;
	
	public static void main(String[] args) {
		//Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	System.out.println(System.getProperty("java.version")); //prints out the java version
            	System.out.println("this is a single .java file");
                Gui.mainMenu(); //starts the gui
            }
        });
	}
	
	public Game(int startCards) {	
		//Initializes players, designed for any number of human players
		for (int i=0; i<Gui.humanNumber; i++) {
			players[i] = new HumanPlayer(deck, startCards);
		}
		for (int i=Gui.humanNumber; i<Gui.robotNumber+Gui.humanNumber; i++) {
			players[i] = new RobotPlayer(deck, startCards);
		}
		
		/*
		 * turns a card over from the top of the pile
		 * if the card turned over was a special card
		 * turn over another card
		 */
		int topCard = deck.dealCard();
		deck.discardCard(topCard);
		while (Rules.isSpecial(topCard)) {
			topCard = deck.dealCard();
			deck.discardCard(topCard);
		}	
		
		//Initializes the gui
		gui = new Gui(this);
		
		//refreshes the gui
		gui.drawScene();
		
	}
	
	/*
	 * main recursive function which calls most other functions
	 * 
	 * each time doTurn is called it conducts the go of the specified player
	 * it then calls place card which will trigger an animation in a new thread
	 * it the returns back while the animation is running
	 * and calls dospecial which implements the special cards in uno then it returns
	 * after the animation thread finishes it will call doTurn again which creates a recursive loop
	 * the loop ends when it reaches the human player
	 */
	public boolean doTurn(int player) {
		int discardedCard;
		if (player<0) {
			player=Gui.playersNumber-1;
		}
		if (player>Gui.playersNumber-1) {
			player=0;
		}
		if (Gui.runningAnimation || Gui.chosingColor) {
			return false;
		}
		
		gui.drawScene();
		
		int index =  player;
		if (player==0 && Gui.humanNumber == 1) {
			index =  gui.buttonPressed;
		}

		discardedCard = players[player].placeCard(this, index);
		if (discardedCard >= 0) {
			Rules.doSpecial(this, discardedCard, player);
			System.out.println("Player "+(player+1)+" placed a "+Rules.getCardType(discardedCard)[0]+Rules.getCardType(discardedCard)[1]);
		} else if (discardedCard ==-244) {
			if (getNextPlayer(player) == 0 && Gui.humanNumber == 1) {
            	return false;
            }
			doTurn(getNextPlayer(player));
		}
		return false;
	}

	//returns the next player number which should be called in the loop
	public int getNextPlayer(int player) {
		if (reverse) {
			if (player==0) {
				return Gui.playersNumber-1;
			} else {
				return player-1;
			}
		} else {
			if (player==Gui.playersNumber-1) {
				return 0;
			} else {
				return player+1;
			}
		}
	}
	
	//gets the hand size of all players and returns them in an array
	public int[] getHandSize() {
		int handSize[] = new int[Gui.playersNumber];
		for (int x=0; x<Gui.playersNumber; x++) {
			handSize[x] = players[x].getHandSize();
			if (handSize[x]==0) {
				System.out.println("Player "+(x+1)+" Won");
				gameOver(x);
			}
		}
		return handSize;
	}
	
	//called when someone wins the game
	//calculates the score if appropriate
	//and calls displayWinner in gui class
	private void gameOver(int winner) {
		if (Gui.takeScore) {
			int totalScore = 0;
			for (int i = 0; i < Gui.playersNumber; i++) {
				if (i != winner) {
					totalScore += players[i].getHandScore();
				}
			}
			players[winner].addPlayerScore(totalScore);
		}
		gui.displayWinner(winner, players);
	}
	
	//toggles the reverse variable
	public void toggleReverse() {
		if (reverse) {
			reverse = false;
		} else {
			reverse = true;
		}
	}	
}











class Gui {
	//the panels for the game view
	private JFrame gameFrame;
   private JPanel masterPanel;
   private JPanel bottomPanel;
   private JPanel leftPanel;
   private JPanel rightPanel;
   private JPanel topPanel;
   private JPanel centrePanel;
   private JPanel animation;
   
   //saves the location of discard and deck for animations
   private JLabel discardLabel;
	private JButton deckButton;
	
	private Clicklistener cardClick; //calls this class whenever a card is pressed
   private Images cardImages = new Images(); //Initializes the images
   private JLabel [] playersText = new JLabel[9];//saves the location of the players
	private Dimension playerCardSize = new Dimension();//saves the size of the players cards for animations
	private final Dimension deckSize = new Dimension(131,200); // the size of the deck the width = 200*0.655=131
	public int buttonPressed = -2;  //stores the card the user clicked on 
	
	//stores the values set on the menu screen
	public static int humanNumber = 1;
	public static int robotNumber = 1;
	public static int playersNumber = humanNumber+robotNumber;
	public static boolean takeScore = true;
	public static boolean fullScreen = false;
	public static boolean sound = true;
	
	public static boolean runningAnimation = false; //used to block the game continuing during a animation
   public static boolean chosingColor = false; //used to block the game continuing while waiting for user input
   public static boolean pickingUp = false; //used to block the game continuing while a user is picking up from a special card
   
   //stores the instance of the Game class
   public static Game game;

   //draws the main menu
   public static void mainMenu() {
   	JButton button;
       JLabel label;
       SpinnerNumberModel model;
       
       //create a local instance of the images
	    Images cardImage = new Images();    
	    Font title = new Font("Verdana", Font.PLAIN, 150);
	    Font h3 = new Font("Arial", Font.PLAIN, 20);
   	
   	//Create and set the window.
       JFrame menuFrame = new JFrame("UNO");
       menuFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    menuFrame.pack();
       menuFrame.setMinimumSize(new Dimension(850,380));
       menuFrame.setSize(new Dimension(1000,800));
       menuFrame.setVisible(true);

       Container pane = menuFrame.getContentPane();
	    pane.setLayout(new GridBagLayout());
	    GridBagConstraints c = new GridBagConstraints();
       
	    //Default constants
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.NORTHWEST;
	    c.insets = new Insets(10,10,10,10);
	    
	    //display a +4 card
	    char [] card = { 'C','W' };
	    label = new JLabel( new ScaledImageIcon(cardImage.getCardImage( card ),257) );
	    label.setPreferredSize(new Dimension(169,257));
	    c.weightx = 0;
	    c.gridwidth = 1;
	    c.gridheight = 4;
	    c.gridx = 0;
	    c.gridy = 0;
	    pane.add(label, c);
	    
	    //display a Color Changing Card
	    card[0] = 'F';
	    card[1] = 'W';
	    label = new JLabel( new ScaledImageIcon(cardImage.getCardImage( card ),257) );
	    label.setPreferredSize(new Dimension(169,257));
	    c.gridx = 5;
	    pane.add(label, c);
	    
	    //display the text UNO
	    label = new JLabel("UNO");
	    label.setFont(title);
	    c.weightx = 0.5;
	    c.gridwidth = 4;
	    c.gridheight = 1;
	    c.gridx = 1;
	    c.gridy = 0;
	    pane.add(label, c);
	    
	    //display my name
	    label = new JLabel("Donovan Webb");
	    label.setFont(h3);
	    c.weightx = 0.5;
	    c.gridwidth = 1;
	    c.gridheight = 1;
	    c.gridx = 0;
	    c.gridy = 3;
	    pane.add(label, c);
	    
	    //display my student number
	    label = new JLabel("19330681");
	    label.setFont(h3);
	    c.weightx = 0.5;
	    c.gridwidth = 1;
	    c.gridheight = 1;
	    c.gridx = 5;
	    c.gridy = 3;
	    pane.add(label, c);
		 
	    
	    //select human number
	    label = new JLabel("Human Players");
	    label.setFont(h3);
	    c.gridwidth = 1;
	    c.gridx = 1;
	    c.gridy = 1;
	    pane.add(label, c);
	    
	    
	    model = new SpinnerNumberModel(1,1,1,1);    
	    //if you add the comment on the below line it block you to play robot vs robot
	    //by setting human players to zero
	    model = new SpinnerNumberModel(1,0,1,1);
	    final JSpinner humanSpinner = new JSpinner(model);
	    c.gridx = 2;
	    pane.add(humanSpinner, c);
	    
	    //select robot number
	    label = new JLabel("Robot Players");
	    label.setFont(h3);
	    c.gridx = 1;
	    c.gridy = 2;
	    pane.add(label, c);
	    
	    model = new SpinnerNumberModel(1,1,8,1);
	    final JSpinner robotSpinner = new JSpinner(model);
	    c.gridx = 2;
	    pane.add(robotSpinner, c);
	    
	    //select the number of starting cards
	    label = new JLabel("Starting Cards");
	    label.setFont(h3);
	    c.gridx = 3;
	    c.gridy = 1;
	    pane.add(label, c);
	    
	    model = new SpinnerNumberModel(7,1,99,1);
	    final JSpinner startCardSpinner = new JSpinner(model);
	    c.gridx = 4;
	    c.gridy = 1;
	    pane.add(startCardSpinner, c);
	    
	    //chose weather to keep score
	    label = new JLabel("Keep Score");
	    label.setFont(h3);
	    c.gridx = 3;
	    c.gridy = 2;
	    pane.add(label, c);
	    
	    final JCheckBox scoreText = new JCheckBox("",true);
	    c.gridx = 4;
	    c.gridy = 2;
	    pane.add(scoreText, c);
	      
	    
	    //chose weather to launch in full screen
	    label = new JLabel("Full Screen");
	    label.setFont(h3);
	    c.gridx = 1;
	    c.gridy = 3;
	    pane.add(label, c);
	    
	    final JCheckBox fullScreenText = new JCheckBox("",false);
	    c.gridx = 2;
	    c.gridy = 3;
	    pane.add(fullScreenText, c);
	    
	    
	    //chose weather to play sounds
	    label = new JLabel("Play Sound");
	    label.setFont(h3);
	    c.gridx = 3;
	    c.gridy = 3;
	    pane.add(label, c);
	    
	    final JCheckBox soundText = new JCheckBox("",true);
	    c.gridx = 4;
	    c.gridy = 3;
	    pane.add(soundText, c);
	    
	    
	    //displays the start game button
	    button = new JButton("Start Game");
	    c.ipady = 40;      //make this component tall
	    c.weightx = 0.5;
	    c.weighty = 1;
	    c.gridwidth = 4;
	    c.gridheight = 1;
	    c.gridx = 1;
	    c.gridy = 4;
	    pane.add(button, c);
	    
	    //if the start game button is pressed it will setup all the variables 
	    //based on the fields in the gui
	    //and initialize a new instance of the Game class
	    
	    button.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
	        	humanNumber = Integer.parseInt(humanSpinner.getValue().toString());
	        	robotNumber = Integer.parseInt(robotSpinner.getValue().toString());
	        	playersNumber = humanNumber+robotNumber;
	        	fullScreen = fullScreenText.isSelected();
	        	sound = soundText.isSelected();
	        	takeScore = scoreText.isSelected();
	        	
	        	new Game( Integer.parseInt(startCardSpinner.getValue().toString()) );
	        	if (humanNumber == 0) {
	        		game.doTurn(0);
	        	}
	        } 
	    });
   }
   
   public Gui(Game game) {
   	
   	Gui.game = game;
   	cardClick = new Clicklistener();
   	
   	//Create and set up the window.
       gameFrame = new JFrame("UNO Game");
       gameFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
       gameFrame.pack();
       gameFrame.setMinimumSize(new Dimension(600,700));
       gameFrame.setSize(1000,800);  
       gameFrame.setVisible(true);

       //makes sure the game window is on top of the menu window
       gameFrame.setAlwaysOnTop(true);
       gameFrame.toFront();
       gameFrame.requestFocus();
       gameFrame.setAlwaysOnTop(false);
       if (fullScreen) {
       	gameFrame.setExtendedState(JFrame.MAXIMIZED_BOTH); 
       }
       //initilizes and adds all the panels to there correct positions
       masterPanel = new JPanel(new BorderLayout());
       topPanel = new JPanel(new GridLayout(2,6));
       leftPanel = new JPanel(new GridLayout(2,1));
       rightPanel = new JPanel(new GridLayout(2,1));
       centrePanel =new JPanel(new GridLayout(1,2));
       bottomPanel = new JPanel(new GridBagLayout());
       masterPanel.add(topPanel, BorderLayout.PAGE_START);
       masterPanel.add(leftPanel, BorderLayout.LINE_START);
       masterPanel.add(rightPanel, BorderLayout.LINE_END);
       masterPanel.add(centrePanel, BorderLayout.CENTER);
       masterPanel.add(bottomPanel, BorderLayout.PAGE_END);
       
       //sets up the animation glass Pane
       animation = new JPanel(null);
       gameFrame.setGlassPane(animation);
       animation.setVisible(true);
       animation.setOpaque(false);

       gameFrame.getContentPane().add(masterPanel); //adds the master panel to the frame
       
       gameFrame.addComponentListener(new ComponentAdapter() {
           public void componentResized(ComponentEvent componentEvent) {
           	/* 
           	 * TODO cache answer as this event is called many times during a window resize 
           	 * And can be slow to resizes
           	 */
           	//draws hand whenever the window is resized
           	drawHand();
           }
       });
       
       gameFrame.addWindowListener(new WindowListener() {
			@Override
			public void windowClosing(WindowEvent e) {
           	if (Gui.humanNumber == 0) {
           		System.exit(0);
           	}
			}
			public void windowOpened(WindowEvent e) {}
			public void windowClosed(WindowEvent e) {}
			public void windowIconified(WindowEvent e) {}
			public void windowDeiconified(WindowEvent e) {}
			public void windowActivated(WindowEvent e) {}
			public void windowDeactivated(WindowEvent e) {}
       });
	    
       //sets up all the circles representing the robot players
	    for (int i=1; i<playersNumber; i++) {
       	PlayerCirclePaint circle = new PlayerCirclePaint();
       	if (i<7) {
           	circle.setPreferredSize(new Dimension(100,100));
       		topPanel.add(circle);
       	} else if (i==7) {
           	circle.setPreferredSize(new Dimension(100,leftPanel.getHeight()/2));
       		leftPanel.add(circle);
       	} else {
           	circle.setPreferredSize(new Dimension(100,leftPanel.getHeight()/2));
       		rightPanel.add(circle);
       	}
       }
	    
	    //sets up all the text to accompany the robot players circles
   	playersText[0] = new JLabel("player 1,  Cards");
   	playersText[0].setLocation(masterPanel.getWidth(),masterPanel.getHeight());
       for (int i=1; i<playersNumber; i++) {
       	playersText[i] = new JLabel("player "+(i+1)+",  Cards");
       	if (i<=7 && ( i>1 || playersNumber <= 7 )) {
       		topPanel.add(playersText[i]);
       	} else if (i==8) {
       		rightPanel.add(playersText[i]);
       	} else {
       		leftPanel.add(playersText[i]);
       	}
       }
   	
       //refreshes the layout of the view
	    gameFrame.validate();
   }
   
   //renders the entire gameFrame
   public void drawScene() {
   	drawPlayers(game.getHandSize());
		drawHand();
		drawDeck(game.deck);
   }
   
   //updates the robot players card count
   public void drawPlayers(int [] handSize) {
       for (int i=1; i<playersNumber; i++) {
       	playersText[i].setText("player "+(i+1)+", "+handSize[i]+" Cards");
       }
       
	    gameFrame.validate();
   }
   
   //draws the deck
   public void drawDeck(Deck deck) {   
	    if (deckButton == null || discardLabel == null) {
	    	//Initializes deckButton and discardLabel
	    	char [] card = { 'B','W' };   	
	    	deckButton = new JButton( new ScaledImageIcon(cardImages.getCardImage( card ),deckSize.height));
	    	deckButton.setName("-1");
	    	deckButton.setPreferredSize(deckSize);
	    	deckButton.setMinimumSize(deckSize);
	    	deckButton.setMaximumSize(deckSize);
	    	deckButton.addActionListener(cardClick);
	    	centrePanel.add(deckButton);
	    	
	    	discardLabel = new JLabel( new ScaledImageIcon(cardImages.getCardImage( Rules.getCardType( deck.getTopDiscard() ) ),deckSize.height));
	    	discardLabel.setPreferredSize(deckSize);
	    	discardLabel.setMinimumSize(deckSize);
	    	discardLabel.setMaximumSize(deckSize);
	    	centrePanel.add(discardLabel);
	    } else {
	    	//changes the card displayed on the discard pile
	    	discardLabel.setIcon(new ScaledImageIcon(cardImages.getCardImage( Rules.getCardType( deck.getTopDiscard() ) ),deckSize.height));
	    }
	    gameFrame.validate();
   }
   
   //renders the players hand
   public void drawHand() {
	    Hand hand = game.players[0].getHand();
	    int handSize = hand.size();
	    JButton button;
   	JSeparator separator = new JSeparator(JSeparator.VERTICAL); //adds spacers to either side of the cards
	    JPanel handPanel = new JPanel(new GridBagLayout());
	    
	    //clears the bottom panel and adds the new panel to it
	    bottomPanel.removeAll();
	    bottomPanel.setLayout(new BorderLayout());
	    bottomPanel.add(handPanel, BorderLayout.CENTER);
	    
	    //calculates the width and height for each card in the players hand
	    playerCardSize.width = bottomPanel.getWidth()/handSize;
	    if (playerCardSize.width<50) {
	    	/* TODO add pages */
	    	playerCardSize.width=50;
	    }
	    if (playerCardSize.width>deckSize.width) {
	    	/* TODO add pages */
	    	playerCardSize.width=deckSize.width;
	    }
	    playerCardSize.height = (int)(playerCardSize.width*1.5357);
	    
	    //calculates the size required on each side of the players hand
	    Dimension sepearatorSize = new Dimension((bottomPanel.getWidth()-playerCardSize.width*handSize)/2, 0);
	    
	    //updates the location of player text to the end of the hand
	    playersText[0].setLocation(playerCardSize.width*(handSize-1)+sepearatorSize.width, masterPanel.getHeight()-playerCardSize.height);
	    
	    //sets the size and position of the start separator
	    separator.setPreferredSize(sepearatorSize);
	    bottomPanel.add(separator, BorderLayout.PAGE_START);
	    
	    //adds all the cards to the handPanel
	    for (int i=0; i<handSize; i++) {
	    	button = new JButton( new ScaledImageIcon(cardImages.getCardImage( Rules.getCardType(hand.get(i)) ),playerCardSize.height));
		    button.setPreferredSize(playerCardSize);
		    button.setMinimumSize(playerCardSize);
		    button.setMaximumSize(playerCardSize);
		    button.setSize(playerCardSize);
		    button.addActionListener(cardClick);
		    button.setName(""+i);
		    handPanel.add(button);
	    }
	    
	    //sets the size and position of the end seperator
	    bottomPanel.add(separator, BorderLayout.PAGE_END);
	    
	    gameFrame.validate();
   }
   
 //updates the gui with the number of cards the players have
   //Displays a pop up with the winner and the score of the winner then exits the game when the user pushes ok
	public void displayWinner(int winner, Player[] players) {
		int handSize[] = new int[Gui.playersNumber];
		String message = "Player "+(winner+1)+" has Won this round";
		if (Gui.takeScore) {
			message = "Player "+(winner+1)+" has Won this round with a score of "+players[winner].getPlayerScore();
		}
		
		for (int x=0; x<Gui.playersNumber; x++) {
			handSize[x] = players[x].getHandSize();
		}
		drawPlayers(handSize);
		
		if (winner == 0) {
			Sounds.playSound(Sounds.winner);
			JPanel winningScreen = new JPanel(new FlowLayout());

			JTextArea winnerLabel = new JTextArea("\r\n"
					+ "░██╗░░░░░░░██╗██╗███╗░░██╗███╗░░██╗███████╗██████╗░\r\n"
					+ "░██║░░██╗░░██║██║████╗░██║████╗░██║██╔════╝██╔══██╗\r\n"
					+ "░╚██╗████╗██╔╝██║██╔██╗██║██╔██╗██║█████╗░░██████╔╝\r\n"
					+ "░░████╔═████║░██║██║╚████║██║╚████║██╔══╝░░██╔══██╗\r\n"
					+ "░░╚██╔╝░╚██╔╝░██║██║░╚███║██║░╚███║███████╗██║░░██║\r\n"
					+ "░░░╚═╝░░░╚═╝░░╚═╝╚═╝░░╚══╝╚═╝░░╚══╝╚══════╝╚═╝░░╚═╝\r\n");
			winnerLabel.setEditable(false);
			winnerLabel.setFont(new Font("Consolas",Font.PLAIN, 15));

			JLabel rocket = new JLabel("🚀🎇");
			rocket.setFont(new Font(Font.SANS_SERIF,Font.BOLD, 150));
			
			winningScreen.add(rocket);
			winningScreen.add(winnerLabel);
			winningScreen.add(new JLabel(message));
			JOptionPane.showConfirmDialog(gameFrame, winningScreen, "Player "+(winner+1)+" has Won", JOptionPane.DEFAULT_OPTION);
		} else {
			Sounds.playSound(Sounds.loser);
			
			JPanel winningScreen = new JPanel(new GridLayout(2,1));
			JTextArea winnerLabel = new JTextArea("\r\n"
					+ "██╗░░░░░░█████╗░░██████╗███████╗██████╗░\r\n"
					+ "██║░░░░░██╔══██╗██╔════╝██╔════╝██╔══██╗\r\n"
					+ "██║░░░░░██║░░██║╚█████╗░█████╗░░██████╔╝\r\n"
					+ "██║░░░░░██║░░██║░╚═══██╗██╔══╝░░██╔══██╗\r\n"
					+ "███████╗╚█████╔╝██████╔╝███████╗██║░░██║\r\n"
					+ "╚══════╝░╚════╝░╚═════╝░╚══════╝╚═╝░░╚═╝\r\n");
			winnerLabel.setEditable(false);
			winnerLabel.setFont(new Font("Consolas",Font.PLAIN, 15));

			winningScreen.add(winnerLabel);
			winningScreen.add(new JLabel(message));
			JOptionPane.showConfirmDialog(gameFrame, winningScreen, "Player "+(winner+1)+" has Won", JOptionPane.DEFAULT_OPTION);
		}
		System.exit(0);
	}

	//displays the pick color dialog and waits for the user to chose then returns the users choice
	//can be called recursively
	public int pickColor() {
   	int output = 1;
   	Gui.chosingColor = true;  //blocks the game from preceding before the user chooses a color
		String[] possibleValues = { "Red", "Yellow", "Green", "Blue" };
		JComboBox colorF = new JComboBox(possibleValues);
		colorF.setSelectedIndex(0);
		
		//Initializes and displays the dialog
		JPanel question = new JPanel(new GridLayout(2,1));
		question.add(new JLabel("Choose your Color"));
		question.add(colorF);
		int result = JOptionPane.showConfirmDialog(gameFrame, question, "Choose your Color", JOptionPane.DEFAULT_OPTION);
		
		if (result == JOptionPane.OK_OPTION) {
			//if the user press OK then return the selected value
	    	String selectedVal = colorF.getSelectedItem().toString();
	    	if (selectedVal == "Red") {
	    		output = 1;
	    	} else if (selectedVal == "Yellow") {
	    		output = 2;
	    	} else if (selectedVal == "Green") {
	    		output = 3;
	    	} else if (selectedVal == "Blue") {
	    		output = 4;
	    	}
	    } else {
	    	//if the user pushes the x on the dialog display another dialog
	    	return pickColor();
	    }
		
		Gui.chosingColor = false;  //allow the game to continue
		
		return output;
	}
	
	/* 
	 * this calls the animation function for placing a card 
	 * player = the number of the player to deal cards from
	 * card = the card to deal
	 * index = the position in the players hand to take the card from only used if player=0
	 */
   public void placeCardAnimation(int player, int card, int index) {
   	Dimension cardSize = playerCardSize;
   	
       JLabel placeAnimLabel = new JLabel();
   	placeAnimLabel.setIcon(new ScaledImageIcon(cardImages.getCardImage(Rules.getCardType(card)), deckSize.height));
   	placeAnimLabel.setSize(deckSize);
   	placeAnimLabel.setName(""+card);
       animation.add(placeAnimLabel);

   	Point start = playersText[player].getLocation();
   	
   	//selects which location in the hand to take from
   	if (player == 0) {
   		start.x = start.x-playerCardSize.width*index;
   		start.y = masterPanel.getHeight()-playerCardSize.height;
   	} else if (player == 8) {
    	start = SwingUtilities.convertPoint(rightPanel, playersText[player].getLocation(), gameFrame);
	}
   	
   	//sets the endpoint to the discard pile
   	Point endTemp = new Point(discardLabel.getX()-discardLabel.getWidth()/2 , discardLabel.getY()+discardLabel.getHeight()/2);
   	Point end = SwingUtilities.convertPoint(discardLabel, endTemp, gameFrame);
   	end.x -= deckSize.width/2;
   	end.y -= deckSize.height/2;
   	
   	//changes both start and end to include the size of the card
   	Rectangle startR = new Rectangle(start.x,start.y,cardSize.width,cardSize.height);
   	Rectangle endR = new Rectangle(end.x,end.y,deckSize.width,deckSize.height);
   	
		System.out.println("starting place animation , for player="+player+", card="+card);
   	doAnimation(startR, endR , 500, placeAnimLabel, player, 1);
   }
   
	/* 
	 * this calls the animation function for picking a card 
	 * player = the number of the player to deal cards to
	 * card = the card to deal
	 * cardsLeft = used for the recursive counter
	 */
   public void pickCardAnimation(int player, int card, int cardsLeft) {
   	Dimension cardSize = playerCardSize;
   	
       JLabel pickAnimLabel = new JLabel();
   	pickAnimLabel.setIcon(new ScaledImageIcon(cardImages.getCardImage(Rules.getCardType(card)), cardSize.height));
   	pickAnimLabel.setSize(cardSize);
   	pickAnimLabel.setName(""+card);
       animation.add(pickAnimLabel);
   	
       //sets start to the location of the deck
   	Point start = new Point(leftPanel.getWidth()+deckButton.getWidth()/2-deckSize.width/2,topPanel.getHeight()+deckButton.getHeight()/2-deckSize.height/2);
   	
   	//sets end to the players hand
   	Point end = new Point(playersText[player].getX()+playerCardSize.width/2, playersText[player].getY());
   	if (player == 8) {
     	end = SwingUtilities.convertPoint(rightPanel, playersText[player].getLocation(), gameFrame);
 	}
   	
   	//changes both start and end to include the size of the card
   	Rectangle startR = new Rectangle(start.x,start.y,deckSize.width,deckSize.height);
   	Rectangle endR = new Rectangle(end.x,end.y,cardSize.width,cardSize.height);
   	
		System.out.println("starting pick animation, player="+player+", ammount="+cardsLeft+", card="+card);
   	doAnimation(startR, endR , 500, pickAnimLabel, player, cardsLeft);
   }
	
   /* 
    * runs the animation using a swingWorker which means it runs on a new thread 
    * start = start coordinates and size of the card
    * end = end coordinates and size of the card
    * time = the duration in ms of the animation
    * label = the JLabel to be moved and scaled
    * playerNum = the players number
    * cardsLeft = counter for recursion it is the number of cards left to deal if it is <1 it is ignored
    */
   
	private static void doAnimation(final Rectangle start, final Rectangle end, final int time, final JLabel label, final int playerNum, final int cardsLeft)  { 
		runningAnimation = true;  //blocks game perceding while running
		label.setBounds(start);  
		label.setVisible(true);
       SwingWorker<Boolean,Rectangle2D.Double> animate = new SwingWorker<Boolean,Rectangle2D.Double>() { 	
           private float xDelta = (float)(end.x-start.x)/time;  //the speed it moves on the x
       	private float yDelta = (float)(end.y-start.y)/time;  //the speed it moves on the y
       	private float widthDelta = (float)(end.width-start.width)/time;    //the speed it moves on the width
       	private float heightDelta = (float)(end.height-start.height)/time; //the speed it moves on the height
       	private boolean running = true;  //makes sure that the while loop dousent start again
       	
       	//changes the start coordinates into a double so that the location can be calculated more accuratly
       	private Rectangle2D.Double curLoc = new Rectangle2D.Double(start.getX(), start.getY(), start.getWidth(), start.getHeight());
           @Override
           protected Boolean doInBackground() throws Exception  
           { 
               while (running) {
               	boolean xDone = false; //records when it has reached its target x
               	boolean yDone = false; //records when it has reached its target y
               	
	                if ( ((curLoc.getX()>=end.x) && (xDelta >= 0)) || ((curLoc.getX()<=end.x) && (xDelta <= 0)) ) {
	                	xDone = true;
	                }
	                
	                if ( ( (curLoc.getY()>=end.y) && (yDelta >= 0) ) || ( (curLoc.getY()<=end.y) && (yDelta <= 0) ) ) {
	                	yDone = true;
	                }
	                
	                if (xDone && yDone) {
	                	running = false;
	                	return running;
	                }
	                
               	if (!xDone) {
               		curLoc.x += xDelta;
               	}
               	if (!yDone){
               		curLoc.y += yDelta;
               	}
               	curLoc.width += widthDelta;
               	curLoc.height += heightDelta;

	                Thread.sleep(1);
	                //System.out.println(curLoc.toString()+", xdelta="+xDelta+", ydelta="+yDelta);
	                publish(curLoc); //sends the data to proccesing
               } 
                 
               return true; 
           } 
 
           @Override
           protected void process(List<Rectangle2D.Double> chunks) 
           { 
				/*
				 * sets the label to its new coordinates and size 
				 * this is called in batches as in it could execute 
				 * like this doInBackground,doInBackground,doInBackground,doInBackground, process, process,doInBackground, process, process, process
				 * that is why it has to get the next item in the list
				 */
           	Rectangle2D.Double val = (java.awt.geom.Rectangle2D.Double) chunks.get(chunks.size()-1); 
               label.setBounds(val.getBounds());;
           } 
 
           @Override
           protected void done()  
           { 
               // this method is called when the background  
               // thread finishes execution 
               label.setVisible(false);
               runningAnimation = false;
               System.out.println("Finished");  
               
               game.gui.drawScene(); //update gui
               
       		if (cardsLeft>1) {
       			//Recursively calls itself each time with one less card left until it only has a card left when it finishes
       			game.players[playerNum].pickUpCards(game, playerNum, cardsLeft-1);
       			return;
       		} else if (Gui.pickingUp) {
       			Gui.pickingUp = false;
       			return;
       		}
       		
       		
       		
       		//makes the game wait for the humans input
               if (game.getNextPlayer(playerNum) == 0 && Gui.humanNumber == 1) {
               	if( game.players[0].getSkip() ) {
               		game.players[0].setSkip(false);
           			game.doTurn(game.getNextPlayer(0));
           		}
               	return;
               }
               
               //Triggers the next robots turn
               game.doTurn(game.getNextPlayer(playerNum));
           } 
       }; 
         
       // executes the swingworker on worker thread 
       animate.execute();
   }
	
	//used to read when a player pushes a card
	class Clicklistener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			JButton source = (JButton) e.getSource();
			
			Sounds.playSound(Sounds.click);
	   	
			Gui.game.gui.buttonPressed = Integer.parseInt(source.getName());
			Gui.game.doTurn(0);
		}
	}

	//used to draw the circle representing a player
	@SuppressWarnings("serial")
	class PlayerCirclePaint extends JPanel
	{	
		@Override
	   protected void paintComponent(Graphics gr) {
	       super.paintComponent(gr);
	       Graphics2D g = (Graphics2D)gr;
	       Shape ring = createRingShape(50, g.getClipBounds().getHeight()-50, 50, 5); 
	       g.setColor(Color.BLACK);
	       g.fill(ring);
	       g.draw(ring);
	   }
		
	//creates a ring with the specified dimensions
	//this is created by generating two circles and subtracting the smaller from the larger
	private Shape createRingShape( double centerX, double centerY, double outerRadius, double thickness) {
	       Ellipse2D outer = new Ellipse2D.Double(
	           centerX - outerRadius, 
	           centerY - outerRadius,
	           outerRadius + outerRadius, 
	           outerRadius + outerRadius);
	       Ellipse2D inner = new Ellipse2D.Double(
	           centerX - outerRadius + thickness, 
	           centerY - outerRadius + thickness,
	           outerRadius + outerRadius - thickness - thickness, 
	           outerRadius + outerRadius - thickness - thickness);
	       Area area = new Area(outer);
	       area.subtract(new Area(inner));
	       return area;
	   }
	}


	//used to scale the images to the specified height
	@SuppressWarnings("serial")
	static class ScaledImageIcon extends ImageIcon {
		ScaledImageIcon(Image image, int height) {
	      super(image.getScaledInstance(-1, height, Image.SCALE_SMOOTH));
		}
	}
}








class Rules {
	//checks if the specified card can be placed on top of the topCard
	public static boolean canDiscard(int trialCard, int topCard) {
		char top[] = getCardType(topCard);
		char trial[] = getCardType(trialCard);
		if (top[0] == trial[0]) {
			return true;
		}
		if (top[1] == trial[1]){
			return true;
		}
		if (trial[1] == 'W') {
			return true;
		}
		return false;
	}
	
	//returns true if the card is a special card
	//returns false otherwise
	public static boolean isSpecial(int selectedCard) {
		char card[] = getCardType(selectedCard);
		switch (card[0]) {
			case 'F':
				return true;
			case 'C':
				return true;
			case 'T':
				return true;
			case 'S':
				return true;
			case 'R':
				return true;
			default:
				return false;
		}
	}
	
	//checks if the card is special and does all the appropriate actions if it is
	public static void doSpecial(Game game, int selectedCard, int curPlayer) {
		char card[] = getCardType(selectedCard);
		int output = 0;
		switch (card[0]) {
			case 'F':
				//wild +4 card
				//prevents the player taking a go while picking up cards or color
				
				Gui.pickingUp = true;
				game.players[game.getNextPlayer(curPlayer)].setSkip(true);//skips the next users turn
				switch (game.players[curPlayer].pickColor(game)) {
					case 1:
						output = 108;
						break;
					case 2:
						output = 109;
						break;
					case 3:
						output = 110;
						break;
					case 4:
						output = 111;
						break;
				}
				//changes the color of the wild card so you can see the chosen color
				game.deck.setTopDiscard(output);
				game.gui.drawScene();
				game.players[game.getNextPlayer(curPlayer)].pickUpCards(game, game.getNextPlayer(curPlayer), 4);
				game.players[game.getNextPlayer(curPlayer)].setSkip(false);
				break;
			case 'C':
				//wild color changing card
				switch (game.players[curPlayer].pickColor(game)) {
					case 1:
						output = 112;
						break;
					case 2:
						output = 113;
						break;
					case 3:
						output = 114;
						break;
					case 4:
						output = 115;
						break;
				}
				//changes the color of the wild card so you can see the chosen color
				game.deck.setTopDiscard(output);
				
				game.gui.drawScene();
				break;
			case 'T':
				//+2 card
				Gui.pickingUp = true;
				game.players[game.getNextPlayer(curPlayer)].pickUpCards(game, game.getNextPlayer(curPlayer), 2);
				break;
			case 'S':
				//skip card
				game.players[game.getNextPlayer(curPlayer)].setSkip(true);//skips the next users turn
				break;
			case 'R':
				//reverse card
				if (Gui.playersNumber != 2) {
					game.toggleReverse();
				}
				break;
		}
	}
	
	/* Reverts a colored wild card back to a wild card */
	public static void revertWildCard(Deck deck, int topCard) {
		char card[] = getCardType(topCard);
		switch (card[0]) {
			case 'F':
				//wild +4 card
				deck.setTopDiscard(0);
				break;
			case 'C':
				deck.setTopDiscard(4);
				break;
		}
	}
	
	//returns the score of the specified card according to the rule of uno
	public static int getCardVal(int card) {
		char [] cardType = getCardType(card);
		switch (cardType[0]) {
			case 'F':
				return 50;
			case 'C':
				return 50;
			case 'T':
				return 20;
			case 'S':
				return 20;
			case 'R':
				return 20;
			default:
				return Integer.valueOf(cardType[0])-48;  //converts the char value to the represented int value
		}
	}
	
	//lookup table to convert the unique number of the card to a card type and color
	public static char[] getCardType(int cardIndex) {
		final char cards[][] = {
				{'F', 'W'}, {'F', 'W'}, {'F', 'W'}, {'F', 'W'}, {'C', 'W'}, {'C', 'W'}, {'C', 'W'}, {'C', 'W'},
				
				{'0', 'R'}, {'1', 'R'},{'2', 'R'},{'3', 'R'},{'4', 'R'},{'5', 'R'},{'6', 'R'},{'7', 'R'},{'8', 'R'},{'9', 'R'},
				{'1', 'R'},{'2', 'R'},{'3', 'R'},{'4', 'R'},{'5', 'R'},{'6', 'R'},{'7', 'R'},{'8', 'R'},{'9', 'R'},
				{'S', 'R'},{'S', 'R'},{'T', 'R'},{'T', 'R'},{'R', 'R'},{'R', 'R'},
				
				{'0', 'Y'},{'1', 'Y'},{'2', 'Y'},{'3', 'Y'},{'4', 'Y'},{'5', 'Y'},{'6', 'Y'},{'7', 'Y'},{'8', 'Y'},{'9', 'Y'},
				{'1', 'Y'},{'2', 'Y'},{'3', 'Y'},{'4', 'Y'},{'5', 'Y'},{'6', 'Y'},{'7', 'Y'},{'8', 'Y'},{'9', 'Y'},
				{'S', 'Y'},{'S', 'Y'},{'T', 'Y'},{'T', 'Y'},{'R', 'Y'},{'R', 'Y'},
				
				{'0', 'G'},{'1', 'G'},{'2', 'G'},{'3', 'G'},{'4', 'G'},{'5', 'G'},{'6', 'G'},{'7', 'G'},{'8', 'G'},{'9', 'G'},
				{'1', 'G'},{'2', 'G'},{'3', 'G'},{'4', 'G'},{'5', 'G'},{'6', 'G'},{'7', 'G'},{'8', 'G'},{'9', 'G'},
				{'S', 'G'},{'S', 'G'},{'T', 'G'},{'T', 'G'},{'R', 'G'},{'R', 'G'},
				
				{'0', 'B'},{'1', 'B'},{'2', 'B'},{'3', 'B'},{'4', 'B'},{'5', 'B'},{'6', 'B'},{'7', 'B'},{'8', 'B'},{'9', 'B'},
				{'1', 'B'},{'2', 'B'},{'3', 'B'},{'4', 'B'},{'5', 'B'},{'6', 'B'},{'7', 'B'},{'8', 'B'},{'9', 'B'},
				{'S', 'B'},{'S', 'B'},{'T', 'B'},{'T', 'B'},{'R', 'B'},{'R', 'B'},
				
				{'F', 'R'}, {'F', 'Y'}, {'F', 'G'}, {'F', 'B'}, {'C', 'R'}, {'C', 'Y'}, {'C', 'G'}, {'C', 'B'},
				
				{'B', 'W'}
		};
		return cards[cardIndex];
	}
}





class Deck {
	final private int cardMax = 108; //size of the deck
	private LinkedList<Integer> deck = new LinkedList<Integer>();   		//stores the deck
	private LinkedList<Integer> discardPile = new LinkedList<Integer>(); 	//stores the discardPile
	private Random rand;

	public Deck() {
		rand = new Random();
		
		/*
		 * goes through each location in the deck and selects a random uno card to place there 
		 * if that card is already in the array then keep selecting 
		 * a new random card until the selected card is not in the array
		 * 
		 * could be more efficient but only triggers when the game starts
		 */
		
		for(int i=0; i<cardMax; i++) {
			int random = rand.nextInt(cardMax);
			while (deck.contains(random)) {
				random = rand.nextInt(cardMax);
			}
			deck.add(random);
		}
	}
	
	//deals a card
	//returns the top card from the deck
	//after removing it from the deck
	public int dealCard() {
		if (deck.isEmpty()) {
			deckEmpty();
		}
		return deck.pop();
	}
	
	/* 
	 * deals numCards amount of cards
	 * same as calling dealCards multiple times
	 * 
	 * returns a linkedList of all the cards dealt
	 */
	
	public LinkedList<Integer> dealCards(int numCards) {
		if (deck.size()<numCards) {
			deckEmpty();
		}
		LinkedList<Integer> output = new LinkedList<Integer>();
		for(int i=0; i<numCards; i++) {
			output.add(deck.pop());
		}
		return output;
	}
	
	//gets the size of the deck
	public int getDeckSize() {
		return deck.size();
	}
	
	public void printDeck() {
		System.out.print("Deck = [");
		for (int i=0; i<deck.size(); i++) {
			System.out.print(Rules.getCardType(deck.get(i))[0]);
			System.out.print(Rules.getCardType(deck.get(i))[1]);
			System.out.print(", ");
		}
		System.out.println("]");
	}
	
	public void printDiscard() {
		System.out.print("Discard = [");
		for (int i=0; i<discardPile.size(); i++) {
			System.out.print(Rules.getCardType(discardPile.get(i))[0]);
			System.out.print(Rules.getCardType(discardPile.get(i))[1]);
			System.out.print(", ");
		}
		System.out.println("]");
	}
	
	//shuffles the deck
	public void shuffle() {
		Collections.shuffle(deck);
	}
	
	/* 
	 * to be called if the deck runs out 
	 * it will move the discard pile to the deck
	 * and then shuffle the deck 10 times
	 * 
	 * if there still is not enough cards
	 * then it will add in a second deck
	 */
	public void deckEmpty() {
		while(discardPile.size()>1) {
			deck.add(discardPile.removeLast());
		}
		if (deck.size()<=4) {
			//adds a second deck in case of +4 card
			for(int i=0; i<cardMax; i++) {
				int random = rand.nextInt(cardMax);
				while (deck.contains(random)) {
					random = rand.nextInt(cardMax);
				}
				deck.add(random);
			}
		}
		
		for(int i=0; i<10; i++) {
			shuffle();
		}
	}
	
	//add the given card to the discard pile
	public void discardCard(int card) {
		discardPile.addFirst(card);
	}

	//returns the card at the top of the discard pile
	public int getTopDiscard() {
		return discardPile.getFirst();
	}

	//changes the card at the top of the discard to the specified card
	//used to set a wild card to a colored variant
	public void setTopDiscard(int card) {
		discardPile.set(0, card);
	}
}







@SuppressWarnings("serial")
class Hand extends LinkedList<Integer>{

	//Initializes the hand with startCards number
	//of randomly dealt cards
	public Hand(Deck deck, int startCards) {
		for (int i=0; i<startCards; i++) {
			super.add(deck.dealCard());
		}
	}
	
	//prints the content of the hand to console
	//used for debugging
	public void printHand() {
		System.out.print("Your hand = [");
		for (int i=0; i<super.size(); i++) {
			System.out.print(Rules.getCardType(super.get(i))[0]);
			System.out.print(Rules.getCardType(super.get(i))[1]);
			System.out.print(", ");
		}
		System.out.println("]");
	}
	
	//add a card to the end of the hand
	//returns the card it adds
	public int addCard(int card) {
		super.add(card);
		return card;
	}
	
	//adds multiple cards and returns all the cards added
	/*public LinkedList<Integer> addCards(LinkedList<Integer> cards) {
		int size = cards.size();
		LinkedList<Integer> output = new LinkedList<Integer>();
		
		for (int i=0; i<size; i++) {
			int curCard = cards.pop();
			output.add(curCard);
			super.add(curCard);
		}
		return output;
	}*/
}









class Player {
	protected Hand hand;  		//stores the players hand
	protected int playerScore;  //stores the total score of the player
	protected boolean skip;  	//identifies if the player should be skipped

	public Player(Deck deck, int startCards) {
		hand = new Hand(deck, startCards);
	}
	
	//place holder for the definitions in the sub classes
	public int placeCard(Game game, int index) {
		return -500;
	}			
	
	/*
	 * deals a card to the players hand then plays the animation 
	 * if the user is a robot it deals with the card face down
	 * 
	 * playerNum is the player who should pickup
	 */
	public void pickUpCard(Game game, int playerNum) {
		int card = hand.addCard(game.deck.dealCard());
		if (playerNum != 0 && Gui.humanNumber == 1) {
			card = 116;
		}
		game.gui.pickCardAnimation(playerNum, card, 1);
	}
	
	/*
	 * a recursive method which will deal cardsLeft number of cards then plays the
	 * animation if the user is a robot deals with the card face down and repeats
	 * this until cardsLeft == 1
	 * 
	 *  playerNum is the player who should pickup
	 *  cardsLeft is the number of cards to pickup
	 */
	public void pickUpCards(Game game, int playerNum, int cardsLeft) {
		int card = hand.addCard(game.deck.dealCard());
		if (playerNum != 0 && Gui.humanNumber == 1) {
			card = 116;
		}
		game.gui.pickCardAnimation(playerNum, card, cardsLeft);
	}
	
	//place holder for the definitions in the sub classes
	public int pickColor(Game game) {
		return 1;
	}
	
	//returns the size of the players hand
	public int getHandSize() {
		return hand.size();
	}
	
	//calculates the players score
	public int getHandScore() {
		int output = 0;
		for (int i=0; i<hand.size(); i++) {
			output += Rules.getCardVal(hand.get(i));
		}
		return output;
	}
	
	//calculates the players score
	public Hand getHand() {
		return hand;
	}
	
	//gets the first occurrence of the card in the players hand
	public int getHandIndex(int card) {
		return hand.indexOf(card);
	}
	
	//gets the players score
	public int getPlayerScore() {
		return playerScore;
	}
	
	//returns wether the player should be skipped
	public boolean getSkip() {
		return skip;
	}
	
	//adds score to the players score
	public void addPlayerScore(int score) {
		playerScore += score;
	}
	
//	sets the player score to score
	public void setPlayerScore(int score) {
		playerScore = score;
	}
	
	public void setSkip(boolean input) {
		skip = input;
	}
}


class HumanPlayer extends Player {
	
	public HumanPlayer(Deck deck, int startCards) {
		super(deck, startCards);
	}
	
	
	/*
	 * places the card at the specified index
	 * 
	 * it removes the card at the specified index from the players hand 
	 * and adds it to the discard pile then it calls the animation 
	 * 
	 * if the specified index is -1 then it picks up
	 * 
	 * if the index is not valid then it returns -500
	 * 
	 * returns -244 = skip
	 * returns -500 = pickup
	 * returns -501 = against rules
	 */
	public int placeCard(Game game, int index) {
		if (skip) {
			skip = false;
			return -244;
		}
		
		int topCard = game.deck.getTopDiscard();
		if (index==-1) {
			pickUpCard(game, 0);
			return -500;
		} else if (Rules.canDiscard(hand.get(index), topCard)) {
			//reverts special cards in the discard pile back to there original card
			if (Rules.isSpecial(topCard)) {
				Rules.revertWildCard(game.deck, topCard);
			}
			
			//can play card will place
			int selectedCard = hand.remove(index);
			game.deck.discardCard(selectedCard);
			game.gui.placeCardAnimation(0, selectedCard, (hand.size()+1)-(index+1));
			return selectedCard;
		} else {
			//against rules will exit
			System.out.println("Against the rules");
			return -501;
		}
	}
	
	/*
	 * asks the use to chose a color and returns the color number chosen
	 * 
	 * returns
	 * 1 = Red
	 * 2 = Yellow
	 * 3 = Green
	 * 4 = Blue
	 */
	public int pickColor(Game game) {
		int choice = game.gui.pickColor();
		game.doTurn(game.getNextPlayer(0));
		return choice;
	}
}









class RobotPlayer extends Player {
	public RobotPlayer(Deck deck, int startCards) {
		super(deck, startCards);
		
		//just used for testing
//		hand.set(0, 54);
//		for(int i=1; i<startCards; i++) {
//			hand.set(i, 41+i);
//		}
	}
	
	/*
	 * decides which card the robot should place 
	 * 
	 * it choose the first card in its hand which can be placed 
	 * according to the rules of uno 
	 * it then removes the card from the players hand 
	 * and adds it to the discard pile then it calls the animation 
	 * 
	 * if it is unable to find any valid cards then it will pick up one
	 * 
	 * playerNum is the player who is playing
	 * 
	 * returns -244 = skip
	 * returns -500 = pickup
	 */
	public int placeCard(Game game, int playerNum) {
		if (skip) {
			skip = false;
			return -244;
		}
		int topCard = game.deck.getTopDiscard();
		for (int index = 0; index<hand.size(); index++) {
			if (Rules.canDiscard(hand.get(index), topCard)) {
				//reverts special cards in the discard pile back to there original card
				if (Rules.isSpecial(topCard)) {
					Rules.revertWildCard(game.deck, topCard);
				}
				
				int selectedCard = hand.remove(index);
				game.deck.discardCard(selectedCard);
				game.gui.placeCardAnimation(playerNum, selectedCard, (hand.size()+1)-(index+1));
				return selectedCard;
			}
		}
		pickUpCard(game, playerNum);
		return -500;
	}
	
	/*
	 * Chooses a color and returns the color number chosen
	 * it will chose the color of most of the cards in its hand
	 * 
	 * returns
	 * 1 = Red
	 * 2 = Yellow
	 * 3 = Green
	 * 4 = Blue
	 */
	public int pickColor(Game game) {
		int color[] = new int[4];
		char cur = ' ';
		for (int c=0; c<4; c++) {
			switch (c) {
				case 0:
					cur = 'R';
					break;
				case 1:
					cur = 'Y';
					break;
				case 2:
					cur = 'G';
					break;
				case 3:
					cur = 'B';
					break;
			}
			for (int index = 0; index<hand.size(); index++) {
				if (Rules.getCardType(hand.get(index))[1] == cur) {
					color[c] += 1;
				}
			}
		}
		int largestC = 0;
		int largestCVal = 0;
		for (int c=0; c<4; c++) {
			if (color[c]>largestCVal) {
				largestCVal = color[c];
				largestC = c;
			}
		}
		return largestC+1;
	}
}











class Images {
	//the official uno colors
	private final Color RED = new Color(237,27,36);
	private final Color YELLOW = new Color(255,222,21);
	private final Color GREEN = new Color(79,170,67);
	private final Color BLUE = new Color(0,114,187);
	
	//the ammount of pixels to crop off the edges of the images
	private final int cropSize = 2;
	
	//creates a local instance of these classes so that i can access
	//the local variables which hold some of the images
	ImagesWild wildCards = new ImagesWild();
	ImagesNumbers numberCards = new ImagesNumbers();
	ImagesOther otherCards = new ImagesOther();
	
	
	/*
	 * gets the specified byte array and then converts it to a image 
	 * then changes it into the specified color,
	 * Then it fills in all the static colors which where previously converted to grayscale,
	 * then returns the final image
	 */
	public Image getCardImage(char card[]) {
		byte [] cardImg;
		boolean isMultiColored = false;
		switch (card[0]) {
			case '0':
				cardImg = numberCards.uno0;
				break;
			case '1':
				cardImg = numberCards.uno1;
				break;
			case '2':
				cardImg = numberCards.uno2;
				break;
			case '3':
				cardImg = numberCards.uno3;
				break;
			case '4':
				cardImg = numberCards.uno4;
				break;
			case '5':
				cardImg = ImagesNumbers.uno5;
				break;
			case '6':
				cardImg = ImagesNumbers.uno6;
				break;
			case '7':
				cardImg = ImagesNumbers.uno7;
				break;
			case '8':
				cardImg = ImagesNumbers.uno8;
				break;
			case '9':
				cardImg = ImagesNumbers.uno9;
				break;
			case 'R':
				cardImg = ImagesOther.unoR;
				break;
			case 'S':
				cardImg = ImagesOther.unoS;
				break;
			case 'T':
				cardImg = otherCards.unoT;
				break;
			case 'C':
				cardImg = ImagesWild.unoC;
				isMultiColored = true;
				break;
			case 'F':
				cardImg = wildCards.unoF;
				isMultiColored = true;
				break;
			case 'B':
				cardImg = ImagesOther.unoB;
				isMultiColored = true;
				break;
			default:
				cardImg = null;
				break;
		}
		
		/* prints out supported image formats */
		/*
		 * for (String format : ImageIO.getReaderFormatNames()) {
		 * System.out.println("format = " + format); } for (String format :
		 * ImageIO.getReaderMIMETypes()) { System.out.println("format = " + format); }
		 */
		
		BufferedImage img = null;
		try {
			img = ImageIO.read(new ByteArrayInputStream(cardImg));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Color cardColor;
		switch (card[1]) {
			case 'R':
				cardColor = RED;
				break;
			case 'Y':
				cardColor = YELLOW;
				break;
			case 'G':
				cardColor = GREEN;
				break;
			case 'B':
				cardColor = BLUE;
				break;
			case 'W':
				cardColor = Color.BLACK;
				break;
			default:
				return img;
		}
		if (isMultiColored) {
			return changeColorSpecial(img, cardColor);
		} else {
			return changeColor(img, cardColor);
		}
		
	}
	
	/*
	 * uses the specified img as a template to create a new image in the specified color 
	 * Then it fills in all the static colors which where previously converted to grayscale,
	 * then returns the image
	 */
	private BufferedImage changeColorSpecial(BufferedImage img, Color newImg) {
		BufferedImage output = new BufferedImage(img.getWidth()-cropSize*2, img.getHeight()-cropSize*2, 1);
	    final int newRGB = newImg.getRGB();
	    for (int x = cropSize; x < img.getWidth()-cropSize; x++) {
	    	int outX = x-cropSize;
	        for (int y = cropSize; y < img.getHeight()-cropSize; y++) {
	        	int outY = y-cropSize;
	        	Color pixel = new Color(img.getRGB(x, y));
	        	float hsbVal[] = Color.RGBtoHSB(pixel.getRed(),  pixel.getGreen(),  pixel.getBlue(), null);
	            if (hsbVal[2] < 0.5) {
	                output.setRGB(outX, outY, newRGB);
	            } else if (hsbVal[2] > 0.5 && hsbVal[2] <= 0.65) {
	            	output.setRGB(outX, outY, YELLOW.getRGB());
	            } else if (hsbVal[2] > 0.65 && hsbVal[2] <= 0.74) {
	            	output.setRGB(outX, outY, RED.getRGB());
	            } else if (hsbVal[2] > 0.74 && hsbVal[2] <= 0.805) {
	            	output.setRGB(outX, outY, GREEN.getRGB());
	            } else if (hsbVal[2] > 0.805 && hsbVal[2] <= 0.86) {
	            	output.setRGB(outX, outY, BLUE.getRGB());
	            } else if (hsbVal[2] > 0.86 && hsbVal[2] <= 0.935) {
	            	output.setRGB(outX, outY, Color.BLACK.getRGB());
	            } else {
	            	output.setRGB(outX, outY, Color.WHITE.getRGB());
	            }
	        }
	    }
	    return output;
	}
	
	//uses the specified img as a template to create a new image in the specified color
	//returns the image in the specified color
	private BufferedImage changeColor(BufferedImage img, Color newImg) {
		BufferedImage output = new BufferedImage(img.getWidth()-cropSize*2, img.getHeight()-cropSize*2, 1);
	    final int newRGB = newImg.getRGB();
	    for (int x = cropSize; x < img.getWidth()-cropSize; x++) {
	    	int outX = x-cropSize;
	        for (int y = cropSize; y < img.getHeight()-cropSize; y++) {
	        	int outY = y-cropSize;
	        	Color pixel = new Color(img.getRGB(x, y));
	        	float hsbVal[] = Color.RGBtoHSB(pixel.getRed(),  pixel.getGreen(),  pixel.getBlue(), null);
	            if (hsbVal[2] < 0.8) {
	                output.setRGB(outX, outY, newRGB);
	            } else {
	            	output.setRGB(outX, outY, Color.WHITE.getRGB());
	            }
	        }
	    }
	    return output;
	}
}

/* 
 * stores the images of the cards in a signed byte array 
 * this is required as the code has to be delivered in a single .java file not a .jar
 * there is a size limit of 65535 bytes for a static in a class
 * and another limit of 65535 bytes for the local variables of a class
 * 
 * this is why i use both static and local varibles in the images classes so that 
 * i can take advantage of both those limits
 */

//the two wild cards
class ImagesWild {
	public final byte [] unoF = {-1,-40,-1,-32,0,16,74,70,73,70,0,1,1,2,0,37,0,37,0,0,-1,-37,0,67,0,3,2,2,3,2,2,3,3,2,3,3,3,3,3,4,7,5,4,4,4,4,9,6,7,5,7,10,9,11,11,10,9,10,10,12,13,17,14,12,12,16,12,10,10,14,20,15,16,17,18,19,19,19,11,14,20,22,20,18,22,17,18,19,18,-1,-62,0,11,8,0,-104,0,100,1,1,17,0,-1,-60,0,29,0,0,1,4,3,1,1,0,0,0,0,0,0,0,0,0,0,8,0,5,6,7,3,4,9,1,2,-1,-38,0,8,1,1,0,0,0,1,-38,-71,25,82,73,37,-76,-32,12,30,-96,-98,-42,-115,-26,44,-4,36,-66,-115,-54,-128,-92,30,73,61,109,-96,2,52,65,94,-88,91,-78,-94,-123,32,-52,78,-57,-99,-59,-126,-34,76,-106,10,118,-108,41,57,-56,-16,-41,112,26,-50,58,76,-86,71,-32,96,82,115,-117,13,-125,-48,23,21,80,12,-56,-47,-109,-123,101,39,56,-77,116,67,27,70,86,-79,-95,26,50,112,-84,-92,-25,17,72,84,84,-84,-37,-5,35,-45,65,-94,-30,32,-108,-100,-7,-23,35,-115,74,-41,93,-73,-53,106,3,54,-70,-90,10,65,-16,-111,-123,-76,-4,87,45,-28,12,-115,-122,-106,-119,-108,-116,27,-11,51,-84,-90,-91,111,36,-26,65,92,-82,40,84,72,-31,21,50,-74,-24,-122,-14,77,115,-108,-93,-118,21,82,-86,-2,-90,87,-122,15,-103,24,37,84,21,81,66,-86,85,11,-82,-43,-56,-24,-123,97,91,-62,-86,40,84,72,-74,-68,94,-95,-64,78,-64,-119,-24,-95,73,78,18,105,105,9,-50,-73,-85,30,-108,38,-66,41,57,-46,72,-38,53,-56,-15,29,49,111,22,38,122,-30,8,84,2,-16,4,-110,-78,100,21,110,35,50,-96,-102,-35,-79,-60,-110,73,101,-54,12,127,-1,-60,0,43,16,0,0,5,4,0,4,6,3,1,1,0,0,0,0,0,0,3,4,5,6,7,0,1,2,54,16,18,38,51,17,20,21,22,52,53,19,32,49,33,34,-1,-38,0,8,1,1,0,1,5,2,-114,99,-112,124,-79,-44,86,-30,112,125,27,93,27,93,27,93,27,93,27,93,27,93,27,93,27,69,-53,52,-114,12,-96,-56,69,80,38,-22,109,-114,-42,85,107,-27,-54,-42,118,58,14,58,-108,-61,-113,-105,-59,38,-40,106,27,115,-97,57,17,19,-71,60,-83,-53,-105,-23,-114,87,-58,-15,83,-88,-45,-127,62,104,-39,-101,122,-100,94,-38,40,62,56,-30,96,92,11,-127,-123,15,-98,124,-114,-12,108,80,92,37,9,14,124,100,24,108,-31,-68,82,35,116,36,-100,85,-29,116,37,108,94,113,-47,-74,-59,65,-11,52,108,-51,-67,78,52,119,2,-98,96,64,-78,40,18,59,-119,33,104,117,67,-98,-104,-100,-104,-124,110,78,114,33,-73,8,55,74,-15,24,28,12,-124,-56,111,123,109,-61,52,108,-51,-67,76,91,-8,14,97,-64,-90,112,2,-90,-58,34,51,81,13,97,-6,58,122,121,116,-94,116,104,-24,36,67,-9,58,93,123,-103,46,-80,-49,17,112,-80,88,88,73,-93,102,109,-22,99,119,-87,-108,-43,17,-44,-82,-98,-98,93,40,-97,7,-35,-4,83,120,54,126,-122,-90,-115,-103,-73,-87,-115,-34,38,80,67,-26,-102,13,-80,-102,-24,-86,11,35,5,-97,-81,-101,-96,-41,77,102,35,-25,-21,120,54,62,-126,-90,-115,-103,-73,-87,-115,-34,-121,91,53,127,-30,-113,-10,-42,-26,-72,64,103,-120,-82,-96,-18,-92,68,-14,81,-108,-34,13,-113,-96,12,-9,-28,83,-102,54,102,-34,-90,-98,-102,42,-70,-70,105,44,19,83,-17,-4,81,-2,-125,-35,80,-54,-2,123,-102,-12,-26,-73,-117,123,-109,42,109,127,-120,12,-123,60,85,-35,51,70,-52,-37,-44,-31,-28,43,-118,-94,-82,-87,116,-32,-67,-30,45,102,111,-50,6,15,117,67,-25,82,41,63,54,71,4,112,-19,-109,-51,98,-51,-74,-52,33,126,107,-51,27,51,111,83,-114,10,-7,86,-101,-89,-77,69,-52,7,-128,32,-110,-1,0,-107,15,-99,77,-113,-125,82,-29,-109,-44,-107,-32,-6,-102,54,102,-75,-71,-101,5,-117,-122,80,-69,-89,-77,-64,63,-85,80,-7,-44,-40,-8,46,-43,-4,27,-120,99,-115,-103,-127,-96,-6,-102,54,102,-90,-77,78,-98,-49,4,-14,-74,50,-109,-109,104,-66,87,-10,-63,106,46,92,50,32,72,110,-37,-71,-43,-22,15,-87,-93,102,106,107,52,-31,43,-103,-116,61,16,122,-12,65,-23,44,28,-117,-112,-31,41,-66,120,-63,-11,52,108,-51,107,-14,-74,11,25,12,-31,122,-16,-81,14,50,4,-127,-104,66,30,79,54,-98,33,82,-125,30,24,-47,65,-120,-115,7,-44,-47,-77,54,-11,56,-103,-45,-128,-127,113,54,116,2,0,-67,37,113,14,84,96,-39,-79,114,-86,-119,101,-43,-127,71,111,-90,-93,24,91,107,16,119,-34,37,35,-23,-118,115,70,-52,-37,-44,-1,0,54,101,-115,-95,76,-30,5,-128,114,-61,127,50,-118,-109,101,44,56,20,23,-122,-88,-63,99,3,-51,-79,-124,-56,49,82,21,1,59,-117,-127,-56,11,100,-92,64,119,37,19,-13,70,-52,-41,-73,51,93,-30,-43,50,-38,86,-3,89,-49,44,-102,-108,-30,-110,-116,43,-112,36,-84,117,54,-114,31,52,-90,44,84,-43,52,-33,79,-102,54,104,-26,70,7,-53,12,-29,65,50,31,70,-41,70,-41,70,-41,70,-41,70,-41,70,-41,70,-41,70,-48,66,-76,1,17,117,-2,-112,-114,-100,-70,-72,105,-62,-93,-1,-60,0,69,16,0,1,2,3,3,6,7,13,7,3,5,0,0,0,0,0,1,2,3,0,4,17,5,18,33,16,19,49,81,116,-78,34,50,65,69,97,114,-93,6,20,32,51,66,113,115,-127,-125,-111,-79,-62,-47,35,82,-110,-95,-63,-31,-16,52,98,-15,21,83,99,-126,-109,-1,-38,0,8,1,1,0,6,63,2,110,-44,-73,91,67,-27,-12,-43,-122,21,-118,66,79,41,-2,126,-63,115,-14,118,84,-70,9,-96,83,-115,-91,56,-57,48,-10,113,-52,61,-100,115,15,103,28,-61,-39,-57,48,-10,113,-52,61,-100,115,15,103,28,-61,-39,-62,90,-107,110,-60,117,-43,104,66,2,9,48,-29,6,-49,-106,107,56,60,99,77,-124,-87,61,32,-62,-92,-26,72,88,-91,-26,-42,60,-76,-21,-117,52,-22,-109,70,-20,41,-55,-84,27,65,41,101,-112,112,64,-6,-57,124,-90,-50,119,55,66,104,104,21,-8,116,-58,98,88,102,-38,111,23,-98,80,-63,-79,1,50,19,115,34,110,-19,111,58,-98,2,-16,-4,-96,-115,94,13,82,104,97,-7,123,71,-19,28,-111,-70,3,-43,-59,96,-21,-9,68,-66,-56,55,-116,89,-37,18,55,98,102,-38,-99,25,-25,37,-97,40,101,-77,-95,39,-17,126,112,-89,42,65,-62,-32,7,8,116,50,-61,108,-117,-41,-35,75,105,-90,113,122,-31,-91,-72,-125,-53,94,-120,-101,-110,105,101,-60,54,-86,-92,-99,56,-118,-62,89,-110,101,-57,-99,86,-124,-95,53,48,-105,45,-57,-60,-96,-81,-118,71,9,84,-13,-24,31,-100,10,74,38,101,116,-95,92,-57,14,-66,-83,16,107,40,-103,101,-46,-127,114,-4,10,122,-76,66,-90,88,61,-15,103,-34,-96,88,-29,35,-83,22,-73,-77,-7,-94,95,100,27,-58,44,-19,-119,27,-80,-19,-115,104,38,-116,-50,-65,86,-35,28,-117,56,80,-61,-71,-27,-95,18,-12,-86,-35,81,-96,0,67,-110,-10,36,-22,-35,-104,74,47,93,-95,76,46,98,-46,-86,24,-105,73,82,-51,-15,-62,-44,34,98,-47,125,29,-21,34,84,51,-117,29,3,4,-114,-102,127,57,33,44,-39,-84,37,20,20,83,-108,-31,47,-50,124,5,-76,-6,18,-29,110,10,41,42,21,4,69,-69,46,-127,70,23,-101,113,-114,-87,-67,-2,34,95,100,27,-58,44,-19,-119,27,-80,-78,62,-15,-126,-52,-44,-4,-37,-83,43,74,22,-15,32,-64,118,77,-41,25,117,58,22,-123,80,-120,9,-76,-89,103,21,102,50,-70,-70,-73,28,39,29,73,-81,44,55,43,34,-38,90,101,-95,68,-92,100,-50,77,-72,-106,-47,-83,81,-3,116,-65,-30,-113,-21,-91,-1,0,28,5,32,-44,24,46,4,-90,-6,-123,10,-87,-119,17,47,-78,13,-29,22,118,-60,-115,-40,95,88,-28,67,52,113,50,-88,-59,-9,82,56,-93,-9,-122,-27,100,91,75,76,-76,40,-108,-116,-77,125,23,126,35,44,-113,-95,25,37,-10,65,-68,98,-50,-40,-111,-69,11,-21,24,106,94,89,55,-99,121,97,8,26,-55,-122,-91,91,-15,-89,-122,-6,-85,90,-81,-106,6,98,-127,61,34,56,-55,-9,66,65,82,113,58,-94,111,-2,-65,-90,89,31,66,50,75,-20,-125,120,-59,-99,-79,35,118,23,-42,48,-19,-79,54,-33,-4,114,-9,-121,-67,95,-89,-65,34,125,113,65,-53,9,42,-90,7,92,76,55,39,69,-87,119,104,43,72,71,126,55,114,-1,0,23,26,-28,-111,-12,34,31,-108,-69,-30,90,67,-105,-85,-90,-15,63,72,-105,-39,6,-15,-117,59,98,70,-20,55,39,42,42,-29,-18,-35,29,29,49,47,42,-43,74,37,-37,8,21,-24,25,19,-21,-124,121,-31,-4,79,-116,49,-92,-60,-97,46,41,-8,70,-125,-18,-119,42,-1,0,-78,35,-70,105,-122,-87,115,56,-38,18,66,-86,8,77,69,127,40,-105,-39,6,-15,-117,59,98,70,-20,78,90,-82,-124,-36,102,-83,53,81,-27,114,-97,119,-58,42,-124,-34,35,92,120,-124,123,-31,43,-69,119,18,33,30,120,127,-46,28,-115,84,-46,-21,105,-28,-114,25,-68,53,82,38,95,98,-22,92,-69,113,-111,95,40,-1,0,43,-22,-117,92,-98,92,-33,-51,18,-5,32,-34,49,103,108,72,-35,-119,106,-75,-102,91,-86,90,-43,-123,47,112,-115,15,-70,-112,-82,-88,-8,-28,-70,-27,-22,-42,-72,8,109,-38,-101,-86,-60,97,15,-6,67,-111,30,-115,57,19,103,75,46,-84,72,-15,-24,120,-50,126,-33,88,-75,-67,-97,-51,18,-5,32,-34,49,102,3,-53,38,-34,-20,54,-52,-72,-70,-37,73,-70,-111,-88,66,-70,-93,-29,-106,79,-87,15,-6,67,-111,30,-115,49,49,54,-78,-100,-32,77,-42,82,124,-91,-99,16,-73,94,81,90,-36,85,-27,40,-99,38,45,111,103,-13,68,-66,-56,55,-116,89,123,35,123,-71,21,-43,31,28,-78,-43,52,33,56,24,37,73,104,-109,-92,-26,-29,-120,-41,-2,113,68,80,4,-116,98,-110,-22,-84,-116,-81,5,-114,13,43,-84,-28,-75,-67,-97,-51,18,-5,32,-34,49,101,-20,-115,-18,-28,-94,60,-95,74,-58,-108,70,-108,67,45,-71,-58,74,113,-54,108,-117,25,-3,98,113,72,-36,-81,-57,-4,-27,-75,-67,-97,-51,18,-5,32,-34,49,102,19,-55,38,-34,-20,54,-4,-70,-81,-76,-22,111,36,-21,30,17,-79,-5,-102,42,114,113,-61,113,-41,91,-60,-93,-5,83,-3,-33,-51,58,46,-38,50,-17,48,-75,10,-47,-44,20,-42,3,82,109,56,-13,-86,-48,-124,38,-92,-63,106,113,-89,25,117,58,80,-76,-48,-120,-75,-67,-97,-51,18,-5,32,-34,49,103,108,72,-35,-121,-84,-119,-57,0,113,-91,-107,75,84,-15,-121,40,-3,125,125,30,2,-98,-99,121,-74,90,78,-107,45,84,16,101,123,-103,82,-40,107,16,-29,-28,81,75,-22,-22,-8,-7,-95,54,-45,-57,57,49,51,121,50,-23,-69,91,-104,-30,124,-16,-71,91,106,92,-51,-77,80,71,6,-108,-11,-63,61,-49,-54,38,93,-57,5,20,-75,18,-93,-26,-58,27,77,-88,-107,-94,97,-66,43,-51,96,72,-44,99,-70,25,75,-41,-5,-39,-28,-73,122,-108,-83,10,-94,95,100,27,-58,44,-19,-119,27,-79,-99,97,106,109,-58,-41,84,-87,38,-124,26,-61,109,91,-46,-71,-38,105,121,-100,14,-113,-69,25,-27,60,-22,23,66,115,37,-77,123,-23,14,38,-57,-112,-22,56,-6,-66,41,31,88,-50,90,-109,46,60,121,1,56,39,-52,50,25,68,-91,109,-67,101,-98,53,112,80,89,-123,27,-60,-107,14,2,107,-7,-62,-41,102,62,38,-103,105,-62,-121,51,99,-54,-114,-3,-76,-51,94,53,18,-20,12,20,-31,-6,69,-67,52,-24,1,115,14,33,106,3,70,55,-94,95,100,27,-58,44,-63,-82,77,-67,-40,117,-89,16,-77,46,-30,-118,-104,115,77,-28,-3,124,41,-92,-103,97,48,-44,-48,23,-123,-21,-92,17,6,82,65,-125,36,-121,124,114,-77,-105,-108,-79,-85,-96,66,-65,-45,-26,-90,37,-17,-15,-77,78,20,-42,2,-89,-97,122,101,-64,40,11,-117,-68,97,-7,-117,71,-20,-36,-98,-70,67,52,-59,0,107,-9,-60,-66,-56,55,-116,55,101,-37,-82,33,-126,-62,104,-61,-22,-63,37,35,-112,-1,0,63,117,34,98,-48,-77,92,66,-123,10,84,-14,77,68,115,15,103,28,-61,-39,-57,48,-10,113,-52,61,-100,115,15,103,28,-61,-39,-57,48,-10,113,-52,61,-100,37,-58,85,97,-95,104,53,74,-123,-54,-125,11,125,-103,-74,38,-36,24,54,-53,46,2,84,127,65,14,78,90,43,-68,-30,-12,14,68,13,66,63,-1,-60,0,40,16,1,0,1,3,3,3,4,3,1,1,1,0,0,0,0,0,1,17,0,33,49,16,65,81,97,113,-16,32,-127,-95,-15,-111,-63,-47,48,-79,-31,-1,-38,0,8,1,1,0,1,63,33,49,40,-112,20,-79,-31,83,6,-35,-23,2,117,78,120,75,-67,-97,-15,-110,73,36,-17,-121,89,-117,5,88,-71,-30,-13,96,20,-82,-25,47,86,33,-77,102,-35,55,-51,110,92,-1,0,-123,33,76,-55,-45,-66,86,-18,-3,-128,26,-110,102,-80,78,-90,-18,-111,122,-3,-50,70,126,94,13,-5,74,24,36,-95,-89,54,5,-62,-17,120,57,-91,103,45,61,32,-112,24,70,-110,-69,-110,-54,-93,-72,-35,-65,123,-76,119,-55,113,-88,-7,-120,-83,97,-18,118,113,-100,-60,56,-99,-112,-78,59,80,111,15,42,54,84,102,99,52,5,-120,9,-120,-70,-44,26,-12,28,64,-128,-11,38,63,-107,-110,-122,-13,123,20,-91,120,82,30,87,28,-120,-57,87,-75,92,77,-18,64,-77,-36,-22,31,-70,-80,-101,-36,-128,103,-71,-43,63,85,51,77,-75,23,17,-8,-110,-35,-92,53,-93,-66,75,-115,72,-60,114,-79,-121,65,-126,29,-100,-39,-78,-24,109,-47,52,-12,-92,-25,-122,39,3,25,67,116,-87,87,121,-63,-60,-105,43,1,-43,-89,51,-101,88,0,75,-104,-119,112,102,48,-94,45,40,11,-101,-17,55,123,77,-93,-48,-12,-125,60,44,-119,92,-73,57,-46,25,91,51,118,98,119,-48,-17,-110,-29,72,-59,18,39,-34,-93,23,-42,-114,-60,-117,82,-33,73,-87,-106,-78,80,4,80,37,-32,89,107,-99,-121,-88,49,-117,124,-29,-6,-11,-33,72,-87,-39,104,43,-23,53,-11,-118,48,-57,-111,40,28,-118,107,4,-64,-68,18,-2,93,14,-7,46,53,-31,57,-47,98,-49,-37,25,46,-16,-125,-35,-122,26,-116,91,-25,31,-41,-82,-6,-96,21,75,61,53,60,-49,26,-99,-14,92,107,-62,115,87,47,112,-60,-120,40,16,-111,-36,24,89,-46,-48,116,57,-87,-88,-87,34,77,3,16,-63,-115,111,-14,-25,87,-127,-29,83,-66,75,-115,120,78,107,-34,-81,-123,-92,-10,35,-58,-78,118,-81,-101,71,27,-118,-126,-123,64,10,-39,91,112,58,74,18,115,69,40,51,-61,40,-50,59,-23,-32,120,-93,36,29,-78,24,-114,-97,-9,-95,-33,37,-58,-74,124,-8,-91,-41,81,-80,93,-24,82,-82,-113,-28,-112,95,-83,100,-19,95,54,-97,11,64,-121,-72,-11,-81,-66,-92,-116,43,-113,-81,67,113,-24,-105,-15,91,100,56,-88,97,-31,-69,-33,67,-66,75,-115,40,-77,66,22,108,-122,-30,64,-21,-17,-91,71,48,-94,-76,76,82,-121,-20,-42,-48,17,38,120,-81,-123,-81,59,-50,-121,13,-36,37,-75,78,-117,-127,26,119,-49,40,13,-103,39,41,120,111,42,92,-95,55,65,-33,37,-58,-73,-20,2,105,72,115,54,30,35,80,44,42,30,71,20,-120,-19,19,-58,107,-50,-13,-89,-119,-29,79,108,4,19,59,-61,11,114,49,-42,-114,-98,24,7,-16,-88,82,-12,-71,-126,-63,127,64,31,45,94,119,-99,60,79,21,123,-32,7,64,-119,39,-106,54,26,85,84,-111,40,-54,-82,-76,119,-61,112,-12,-128,92,-107,-30,118,41,65,43,95,75,-88,-59,118,34,49,78,-25,20,46,78,39,-18,-106,-24,22,25,-12,81,-33,13,-61,68,-29,97,11,18,51,95,116,-1,0,43,-18,-97,-27,0,49,11,87,-104,-117,15,-53,-31,56,122,104,-23,-31,-120,127,10,44,64,67,-119,46,53,19,-102,-121,5,67,-125,92,-21,24,33,-75,-81,100,-19,-118,4,-64,-27,0,-26,-11,45,-12,-102,-119,123,5,67,125,38,-90,94,-29,-83,29,-14,92,104,-102,-10,68,-70,-89,-44,103,41,-69,111,67,5,13,-30,-9,106,9,111,58,3,-79,-68,-37,13,-105,1,-91,12,70,65,-25,116,37,-74,-98,108,-114,-46,100,-56,-36,23,56,-73,90,-79,97,-28,5,-32,-90,14,-39,-73,21,63,-82,35,-33,115,23,38,-1,0,108,-128,-111,-16,82,38,54,-48,-17,-110,-29,67,-74,65,-31,88,-115,108,-79,-36,40,89,42,-54,-71,-72,95,20,-20,62,-13,-87,-76,-103,61,-9,-38,-90,33,49,-13,-65,-111,-14,-23,80,-65,-74,-50,-47,99,6,13,47,35,9,-71,40,-9,-55,-9,107,72,0,48,-74,84,-11,-69,-27,-126,92,29,-53,-105,45,79,-94,-24,71,-24,4,-35,-1,0,-63,-59,-101,105,41,49,-45,67,-90,-50,9,-16,-84,28,-63,105,-73,99,11,73,-6,79,82,25,-123,-56,88,70,27,93,-37,-118,-107,-54,29,-108,-40,58,-100,-10,-103,-37,123,-38,28,76,103,45,75,-74,30,3,-126,123,-76,21,-36,-106,85,29,-50,-51,-69,-40,-93,-82,37,22,2,-106,124,8,97,-33,-67,24,42,-115,73,-111,39,31,-29,36,-110,72,30,42,-55,22,17,-26,-124,103,112,20,-52,123,-113,-3,96,112,-32,-121,109,-119,108,30,94,-65,-1,-38,0,8,1,1,0,0,0,16,-1,0,-61,31,-38,-118,-125,-14,-94,-69,-19,-33,104,-117,65,105,-126,-40,27,-56,-42,14,-91,23,18,39,-99,103,59,-7,-1,0,-7,-1,-60,0,36,16,1,1,0,2,2,2,2,2,3,1,1,0,0,0,0,0,1,17,0,33,49,81,16,65,97,-16,32,113,48,-127,-15,-95,-111,-1,-38,0,8,1,1,0,1,63,16,-68,-70,-70,-127,74,-78,-83,11,94,38,15,-11,-43,120,80,-120,48,-109,-95,-21,-8,118,-37,109,-83,66,53,14,43,-112,-64,93,122,23,5,70,-68,-32,-44,1,24,-5,30,17,20,70,-1,0,117,-128,-122,-91,-80,43,-123,68,69,-94,-8,31,113,-79,-117,-8,45,10,28,104,10,-126,-125,70,1,19,-19,-37,93,-124,122,51,102,-112,105,94,38,5,28,-51,-16,-84,118,-118,29,-125,0,-71,89,110,106,0,48,37,61,17,111,38,106,-106,118,51,-15,74,-37,97,38,72,-62,114,69,-107,-13,97,-67,9,116,124,-127,102,66,13,119,-53,123,-62,-126,-120,-57,106,102,41,97,67,57,26,-110,77,63,-35,-51,53,-128,-56,-10,78,-56,85,93,21,116,-117,90,15,68,106,-67,105,-41,120,29,105,-70,72,-115,33,100,-125,44,-88,51,4,72,-2,43,1,88,10,-4,11,-106,57,72,-108,-112,-48,5,17,-20,9,73,0,99,-69,-96,-94,68,0,44,57,-35,36,33,-114,-18,-128,11,85,20,-79,-25,80,-4,-27,-78,-58,-112,0,-85,59,-120,91,-4,35,5,-103,-78,45,17,96,7,112,-127,-14,17,-52,67,26,65,22,-43,32,6,-35,107,-103,-68,37,-77,-11,26,-93,58,82,-35,-2,-15,6,-18,-12,-115,-128,6,68,-69,3,87,4,90,85,-62,44,-114,-104,108,-95,114,111,25,85,-112,-110,-37,33,-22,-128,16,60,-99,42,102,1,16,-46,34,-120,-23,28,97,116,-13,-47,-7,-70,21,-107,-31,105,-28,44,-59,-64,-32,49,26,-62,-97,-96,-31,67,72,49,4,-66,-63,-50,41,-98,102,42,-95,40,-93,-16,-90,20,-94,43,-13,76,41,39,4,-75,-72,32,46,17,-64,-27,85,-38,42,-87,84,-86,-86,-66,26,33,2,9,89,-73,-42,-48,-2,-50,-4,36,50,5,28,46,44,-115,-92,-63,-68,-23,4,87,56,-93,-121,3,14,95,33,102,125,-105,111,3,-95,-57,-109,98,58,6,-10,-109,-128,44,1,112,-114,7,42,-82,-47,85,74,-91,85,85,-14,52,-128,5,-31,72,127,107,-32,99,-84,106,-54,-81,-73,-16,5,-103,-10,93,-79,-31,21,98,16,106,-127,84,42,-121,110,17,67,6,-63,38,-48,-122,2,26,41,-78,-87,54,-127,19,38,-33,-99,-25,-7,60,42,-55,16,-79,102,42,-98,83,-13,65,5,-103,-10,93,-77,-6,-31,-3,41,59,-80,103,-3,-20,-5,-113,-116,9,-30,7,107,-113,-11,-11,-44,14,-3,-28,-115,-81,106,78,-56,13,15,57,-54,-116,-128,-75,123,25,-61,-98,-4,-124,-26,68,-123,-12,59,92,-74,-17,-48,-101,-16,22,98,38,-32,-32,47,68,-91,-120,12,71,-42,56,104,-20,77,21,0,48,110,7,-24,-49,-5,-39,-9,31,25,-10,125,-31,-40,-125,88,-69,-25,-6,60,127,-25,49,78,127,-19,49,-120,-42,-22,-12,24,-92,20,9,32,31,72,-125,52,105,89,95,1,102,39,22,-56,-92,-83,-27,68,-47,3,-96,-54,44,16,75,65,61,-3,-9,-92,3,-72,-104,91,126,120,-38,86,-61,-68,-5,62,-13,-23,-5,-8,47,39,75,110,-2,-1,0,24,112,-45,-47,-65,-37,-67,97,10,10,-59,73,16,-123,86,24,-90,-86,32,117,-125,-75,83,-28,44,-49,-4,-52,-75,-7,88,104,-28,70,-89,-109,-116,48,25,32,-120,61,-89,88,100,-95,-64,-99,50,-36,-6,126,-2,62,-101,-65,-114,91,-73,56,-98,113,104,13,4,123,60,-80,80,30,-112,118,34,-26,-31,-85,-39,123,9,88,1,85,123,-4,14,-5,79,-116,-6,126,-2,62,-101,-66,86,75,6,-92,77,-64,14,-91,111,56,-60,-21,106,-98,-112,109,85,85,121,-2,6,10,20,-31,124,4,11,47,34,123,-60,106,-92,116,114,-83,-37,-31,-47,-106,-128,64,-125,-128,-32,3,-125,-42,32,-108,42,-89,34,-37,44,50,108,-118,-4,88,40,82,10,81,56,-92,51,-115,120,81,92,68,108,-23,124,-8,80,21,64,57,92,-5,-14,-128,11,-3,33,-45,-62,-86,-86,-86,-14,-65,-125,5,65,-29,7,64,-82,25,-79,-6,-126,64,-126,81,24,-125,-34,35,-128,127,102,127,-99,-97,-25,120,80,21,64,57,92,-101,-80,-62,-117,-78,82,-48,-37,99,-109,-126,-87,-90,-91,80,33,20,81,47,99,-42,113,76,-13,49,81,43,1,95,-127,115,-118,103,-103,-128,-120,74,34,124,35,-8,48,89,-127,-69,-5,104,116,-105,-108,36,64,-92,-14,76,16,107,-6,5,64,42,-127,-14,-122,59,10,13,3,16,53,115,78,99,5,49,-73,26,-46,30,5,36,61,29,46,-71,126,124,-96,21,-86,38,-10,91,16,-23,70,-85,-123,-9,106,5,-112,82,40,-82,24,-33,109,84,-94,-86,2,-93,74,54,76,82,44,7,1,-51,-50,-46,-54,-50,-33,33,102,44,42,-77,-96,1,-79,16,68,-40,-104,-40,1,-97,24,40,2,-44,69,-96,-111,16,116,30,-95,8,58,-124,-29,-91,118,-116,-68,-112,-15,-100,127,-56,19,-86,-9,-121,47,101,-40,96,29,43,-108,-123,74,-41,126,33,2,32,-64,103,-80,70,56,-128,-42,-116,22,-120,116,77,20,56,3,124,-13,-66,-82,52,-124,42,14,82,61,-24,-54,29,57,70,54,-96,39,-78,113,10,-102,19,-107,-54,122,-8,-76,31,-126,-88,23,85,95,-105,-64,91,-102,37,-99,39,-112,22,-11,-69,64,32,-110,36,69,16,-95,-4,73,-126,-94,-34,18,77,13,26,118,18,35,38,-48,-42,33,-85,115,114,10,96,-92,48,81,21,-95,71,92,-77,77,23,-118,-9,-125,121,-106,25,81,-110,-123,73,-14,-9,-110,48,-100,-127,109,124,88,-21,64,93,-97,0,89,-53,-85,-88,20,-125,34,-64,-111,-30,-32,74,-36,-121,34,-112,-95,68,-10,63,-61,-74,-37,108,-104,116,126,20,77,-127,4,77,-119,-108,-15,73,112,98,-84,37,82,1,-95,48,80,8,-126,-120,-69,-89,105,-43,85,85,84,-81,-1,-39,};
	public static final byte [] unoC = {-1,-40,-1,-32,0,16,74,70,73,70,0,1,1,2,0,37,0,37,0,0,-1,-37,0,67,0,3,2,2,3,2,2,3,3,3,3,4,3,3,4,5,8,5,5,4,4,5,10,7,7,6,8,12,10,12,12,11,10,11,11,13,14,18,16,13,14,17,14,11,11,16,22,16,17,19,20,21,21,21,12,15,23,24,22,20,24,18,20,21,20,-1,-62,0,11,8,0,-103,0,100,1,1,17,0,-1,-60,0,29,0,0,2,3,0,3,1,1,0,0,0,0,0,0,0,0,0,0,6,5,7,8,1,3,4,9,2,-1,-38,0,8,1,1,0,0,0,1,120,-76,82,-128,0,-103,-102,-7,-1,0,-12,31,-26,-67,-74,-37,-101,58,67,-109,-115,-77,120,-42,-103,-122,-56,127,-57,-117,86,5,-48,-52,-39,80,66,109,74,-85,61,-6,95,50,6,-106,100,83,-99,113,-10,50,88,-11,86,91,-76,115,-51,-11,-52,104,-7,36,89,-115,117,86,0,52,43,52,25,-19,-80,65,-15,-18,-86,-64,15,90,21,60,27,88,65,-15,-18,-86,-64,27,21,36,59,44,-113,-48,89,-51,53,86,100,125,-115,9,-57,32,-26,-62,116,-86,-86,85,0,44,73,32,-104,69,-43,74,-71,-79,16,61,-10,-128,29,41,91,83,-116,33,-25,7,102,-112,-100,-51,22,6,-44,-120,-61,-63,-37,106,119,31,-88,-84,-81,-96,54,-92,6,38,6,39,-48,-101,-57,49,-6,3,106,71,-31,-1,0,57,101,-54,14,-72,-42,-27,-25,-57,-75,21,115,-51,111,37,102,-9,-51,-27,43,49,-30,49,127,105,-43,89,-102,-29,-31,-98,-90,-50,-98,77,125,31,-32,-128,-38,-117,-1,0,54,-6,-104,22,-70,66,-15,97,-49,26,-22,-36,81,113,90,0,3,-42,-27,-13,-55,71,-23,115,40,0,6,15,-90,-1,0,-1,-60,0,38,16,0,1,3,3,3,4,3,1,1,0,0,0,0,0,0,0,4,3,5,6,0,2,7,16,23,54,1,18,20,22,19,21,32,53,48,-1,-38,0,8,1,1,0,1,5,2,-57,-16,1,26,27,94,31,-37,-93,-24,110,-116,102,-73,70,51,91,-93,25,-83,-47,-116,-42,-24,-58,107,116,99,53,-70,49,-102,-35,24,-51,52,-50,88,-33,11,121,-113,-126,-2,36,-106,50,76,117,-31,-19,-50,-58,86,-121,7,2,29,76,-115,64,-57,93,-73,110,89,92,-59,89,43,-57,87,-11,-118,102,4,63,-122,-88,-88,-81,118,81,-32,-72,-14,42,-101,-63,71,-102,-77,-45,-117,101,-99,19,42,76,-83,-117,-55,42,59,6,119,-109,80,-111,-72,-93,21,-63,-54,-108,10,-118,61,23,116,37,88,-52,85,-62,-62,-68,-90,-78,-113,5,96,82,-21,49,-69,24,-3,-22,-100,-19,108,97,-110,-94,-40,-23,6,-79,94,95,-81,112,-21,77,8,-9,-81,66,-108,-96,107,53,69,-122,30,79,89,71,-126,-61,100,33,14,1,46,-111,-24,-102,15,-14,19,100,-57,-60,34,8,69,-123,112,113,89,-48,-83,27,-48,-8,6,-46,56,-81,120,53,-108,120,46,-104,-14,32,-99,-125,-68,59,40,-14,110,-127,-95,-28,17,-84,95,76,-93,-63,106,31,22,34,86,-20,-4,-20,-111,86,106,-48,-121,106,122,-59,-12,-54,60,22,-109,109,-78,25,19,-43,59,62,75,-19,-73,-91,-106,-21,28,-77,-83,-96,86,81,-32,-72,-63,-118,-57,39,-57,35,-82,114,59,86,-108,-69,-56,-4,54,-69,-118,-125,-83,101,30,10,13,-98,-73,-114,-2,-58,-66,-58,-66,-58,-66,-58,-101,-43,-8,69,-14,-21,-53,-81,46,-102,-81,-75,114,-15,-5,-41,-80,-28,-102,-108,49,-5,35,28,-64,-69,47,116,-44,17,-4,-110,127,18,-13,-6,70,-94,56,87,-108,-24,66,-9,-108,70,-84,72,118,-91,-85,48,31,98,124,-22,69,-20,-46,60,43,-54,105,-31,123,-59,105,-43,43,58,-86,-94,41,116,69,45,45,-73,-83,-9,79,-97,18,-113,-80,-42,21,-27,53,32,-2,14,-84,-120,-9,-107,-85,-127,35,-61,90,79,60,-121,67,43,10,-14,-102,112,23,-50,0,-124,47,21,125,27,-121,-15,-124,-46,-53,4,-117,55,72,-97,-120,-110,59,71,-15,-8,-128,6,68,77,-94,80,46,21,-27,53,40,124,-11,-74,57,51,95,66,-109,-90,-15,-68,-94,-87,4,47,37,83,-52,111,-124,11,38,-107,31,43,51,24,-78,89,79,5,45,-43,40,42,10,-5,54,22,-27,85,-108,120,44,27,32,-35,28,-78,-24,115,83,-14,45,16,-121,33,-107,37,-91,-67,-107,39,-4,-88,-120,-87,20,90,-25,47,80,-12,-84,26,10,-28,-89,82,74,-103,45,108,78,59,-123,121,77,63,-77,-90,-2,-52,-32,-34,67,89,-126,-106,-72,43,-5,99,-35,42,-75,-28,43,-84,26,114,51,80,78,83,-74,-122,91,74,45,115,-105,-61,-47,123,-64,13,-26,86,11,25,88,-6,110,-124,-115,-75,-18,34,-47,35,87,107,-93,53,-75,-47,-102,-38,-24,-51,109,116,102,-74,-70,51,91,93,25,-83,-82,-116,-42,-41,70,104,92,111,28,13,119,71,97,25,67,-107,-56,20,-109,61,-12,-88,-25,-15,-65,-57,43,114,-86,-1,-60,0,73,16,0,1,2,3,4,3,10,11,4,8,7,1,0,0,0,0,1,2,3,0,4,17,5,18,33,49,16,65,81,19,32,34,50,53,97,113,-127,-93,-46,20,35,66,82,116,-125,-111,-79,-78,-77,-16,6,51,-126,-95,21,36,98,114,-110,-63,-47,-31,48,52,67,84,115,-108,-62,-15,-1,-38,0,8,1,1,0,6,63,2,102,110,113,-124,63,104,60,-112,-65,24,-102,-18,92,-62,3,-77,-13,77,-53,36,-15,65,-29,43,-96,102,115,-114,82,-20,28,-18,-57,41,118,14,119,99,-108,-69,7,59,-79,-54,93,-125,-99,-40,-27,46,-63,-50,-20,114,-105,96,-25,118,57,75,-80,115,-69,28,-91,-40,57,-35,-127,43,39,62,-105,31,34,-95,10,66,-111,94,-118,-120,113,-119,-55,116,-72,21,-27,-45,-124,-98,112,97,-7,37,33,107,74,77,91,114,-17,29,58,-116,77,-49,46,-19,24,108,-82,-22,-107,118,-15,-44,43,-50,112,-121,102,-90,-99,83,-49,-70,106,-91,-86,63,72,-37,-81,59,39,44,-24,-3,89,-106,-66,-11,-33,-38,-60,101,-3,125,-86,22,124,-52,-13,51,74,-5,-93,54,81,-71,-41,-98,-125,-86,22,-45,-120,45,-72,-125,117,73,80,-95,7,126,-4,-108,-21,-127,-55,-103,80,46,-72,85,-61,113,28,-3,27,121,-57,93,92,105,11,59,84,-102,-59,-91,-22,-2,98,97,118,-108,-14,-125,86,100,-118,-126,-105,121,53,14,-85,-51,-39,-45,-46,54,-62,-100,85,106,-77,68,-89,-51,27,34,85,35,32,-76,-120,-75,92,109,97,-58,-41,52,-22,-110,-92,-102,-126,47,29,1,114,-78,-5,-100,-73,-5,-105,-8,45,-21,-10,-27,-86,56,104,126,-39,-104,72,6,-14,-51,26,-66,57,-74,30,123,-48,89,-112,-77,-92,-91,90,90,-22,27,109,-70,99,-43,30,15,105,-55,49,53,46,115,23,114,59,70,-61,-100,46,123,-20,-19,-14,-90,-15,114,68,-102,-102,83,-55,-41,94,108,107,-85,100,77,122,26,-66,52,104,-76,-67,95,-52,76,58,18,-94,2,-83,58,42,-102,-58,-26,33,78,-97,39,1,19,22,-119,90,83,54,-22,75,50,-115,-98,49,86,-75,-113,-35,-2,-38,26,-74,126,-48,-101,-88,28,36,89,-27,56,-109,-28,-34,-18,-5,117,-120,12,-80,60,26,65,-79,113,-74,17,-128,-89,63,-42,26,10,-4,-51,1,-58,-51,15,-66,38,62,-48,75,59,117,51,-116,93,84,-67,-63,75,-43,21,85,122,-67,-75,-47,105,122,-65,-104,-104,-100,-79,-19,85,-106,100,-90,14,-22,-36,-62,81,123,113,112,107,-91,43,-114,3,-1,0,-75,-128,-109,60,45,-105,104,72,102,83,34,113,-91,-27,87,15,126,81,-31,83,-85,10,114,-105,82,18,-102,4,-90,-92,-45,-13,-122,-19,-85,105,-69,-45,-89,25,89,37,102,-109,-25,43,-97,-35,-45,-110,-97,125,87,-108,117,106,3,96,-46,43,-102,-79,58,74,9,-83,-59,101,-80,125,87,69,-91,-22,-2,98,116,-89,-19,45,-90,72,-108,97,87,-27,-38,70,107,80,57,-98,-65,-86,102,95,112,4,-7,41,72,-14,70,-108,-92,-27,-81,121,51,-8,127,-98,-117,75,-43,-4,-60,-24,67,8,73,-16,100,16,-87,-121,114,-72,-113,-21,-78,37,-28,-92,-110,91,-77,-27,83,113,-79,83,-115,5,55,-118,112,-7,89,111,38,127,15,-13,-47,105,122,-65,-104,-99,12,89,-23,74,83,105,78,11,-13,75,28,110,-116,-1,0,14,-50,54,-35,-30,82,51,38,-112,18,50,24,111,9,57,41,100,-115,22,-105,-85,-7,-119,-123,79,-52,127,-109,-77,70,-18,-77,-5,94,79,-72,-97,-61,-49,15,76,-81,2,-77,-106,-63,-88,111,47,-22,64,-34,-117,5,37,75,-101,106,95,-62,22,69,10,70,57,116,-29,-20,-47,105,122,-65,-104,-104,-111,104,-34,15,-38,46,-8,66,-101,56,-43,58,-87,-44,27,61,113,-60,-4,-29,-119,-7,-57,19,-13,-114,39,-25,9,5,20,81,-60,-29,28,88,-30,-57,22,1,114,-21,108,52,55,87,86,-77,-63,9,27,98,-41,-97,-69,113,46,-53,42,-32,-89,-110,20,-128,-102,-13,-48,13,19,54,118,-19,-32,-5,-75,-33,25,118,-11,40,-96,114,-22,-127,37,47,-124,-100,-118,4,-69,72,-57,10,103,-97,-77,-88,111,16,-118,84,102,122,55,-91,32,-2,-67,106,-115,-52,97,-59,107,-54,-10,-126,63,-117,-102,38,-67,13,95,26,52,-72,-13,-122,-13,-114,40,-87,71,105,-34,45,-45,-27,96,48,-34,33,-77,-9,99,-124,-66,-120,-104,-103,73,-84,-70,60,83,31,-72,53,-27,-81,19,-41,19,94,-122,-81,-115,26,39,94,108,-35,113,-74,86,-92,-99,-124,13,-30,80,51,81,-92,37,9,-56,105,9,72,-68,-93,-128,2,13,-117,46,-19,109,41,-86,25,-85,-65,-23,-93,-51,-88,-37,-77,101,118,-115,19,94,-122,-81,-115,26,45,31,71,115,-31,59,-62,-67,72,27,-61,104,-50,-35,-16,-27,-125,-32,-110,-54,21,-86,-10,-111,-11,78,-102,67,-77,83,78,-105,-90,29,53,82,-43,-81,68,-41,-95,-85,-29,70,-119,-103,107,-41,55,102,-44,-35,-22,86,-107,16,-29,46,11,-82,54,-94,-107,13,-124,105,64,-14,-113,8,-23,85,-83,107,42,-27,-33,-69,107,-54,39,86,30,119,-69,-36,-4,-20,-62,-107,-61,60,4,19,93,-51,26,-110,33,-71,-49,-76,8,113,111,-69,-62,110,-49,74,-82,-47,52,-51,-51,99,-21,-100,7,-47,97,74,-86,66,-46,-105,-31,-91,-91,56,84,-103,-124,116,-100,-113,-41,68,-41,-95,-85,-29,70,-119,-101,71,113,-16,-115,-58,-17,-117,-67,118,-75,80,25,-11,-62,45,-23,15,25,103,78,-128,-31,-90,37,-91,28,-62,-70,-1,0,60,52,37,62,78,106,-24,-48,-106,-37,73,90,-43,-112,17,-31,118,-125,-119,126,126,-19,89,-111,73,-31,87,81,-4,-77,-9,-31,9,126,117,66,-120,20,67,77,-32,-124,116,8,-102,-73,38,26,75,-88,-107,-15,114,-55,80,-83,94,-37,-98,-84,61,-68,-48,-21,-50,95,83,-85,-62,-68,-15,42,-83,-51,87,82,22,84,105,-105,4,-60,-41,-94,43,-29,70,-117,75,-43,-4,-60,-62,-28,103,-37,84,-27,-110,-80,124,88,-60,-93,-94,-70,-114,-49,-94,94,-80,45,54,-36,60,98,-46,-105,120,38,-71,13,-87,-41,-100,58,29,109,9,-87,-96,114,-16,-91,54,-19,-58,55,91,94,-46,106,88,20,-88,-124,5,80,-102,108,-82,125,0,66,-27,62,-50,75,-8,62,56,-50,56,49,56,-100,-127,-9,-97,100,41,-23,-121,-106,-5,-54,-51,-57,21,121,71,-81,69,-101,-71,-90,-98,16,-29,-82,-72,118,-88,42,-17,-72,8,75,45,-117,-28,96,0,-52,-104,92,-118,-107,91,82,-46,79,11,115,80,-15,45,-125,-120,-37,-62,-53,97,-57,102,51,94,-122,-81,-115,26,38,-28,29,-63,47,34,-127,94,105,-44,122,-115,33,-39,89,-90,-108,-52,-61,70,-118,66,-95,47,75,-68,-29,15,39,39,27,85,-43,14,-72,-27,-119,-1,0,-5,43,-2,-80,-73,29,90,-100,113,102,-14,-108,-93,82,78,-16,-39,86,-88,116,-55,23,47,-76,-13,120,-106,78,-68,60,-35,120,115,-25,88,-104,-3,5,-69,-51,-49,-85,-126,-119,-71,-108,-117,-120,-82,106,72,-50,-67,35,-5,-87,-23,-121,-106,-5,-54,-51,-57,21,121,71,-82,30,-75,-90,91,40,118,100,92,100,43,15,23,-99,122,-51,63,-121,-98,18,-60,-53,-88,66,-44,-101,-12,83,-128,97,-12,33,-87,119,-35,9,-76,-102,77,-43,-91,88,110,-97,-76,54,-61,110,-38,18,105,125,-58,-59,-44,-82,-15,73,-89,81,-114,77,-19,-36,-17,71,38,-10,-18,119,-93,-109,123,119,59,-47,-55,-67,-69,-99,-24,-28,-34,-35,-50,-12,114,111,110,-25,122,57,55,-73,115,-67,28,-101,-37,-71,-34,-124,-70,-117,49,5,73,-44,-30,-44,-76,-5,9,-92,46,102,113,-28,-78,-54,53,-99,124,-62,38,39,-105,-126,9,-70,-46,60,-44,106,26,37,-70,-3,-1,0,-31,43,-2,63,-3,43,71,-1,-60,0,40,16,1,1,0,1,3,3,3,4,2,3,0,0,0,0,0,0,1,17,33,0,49,65,16,81,97,32,-127,-79,113,-95,-16,-15,48,-63,-111,-47,-31,-1,-38,0,8,1,1,0,1,63,33,78,96,18,33,-56,110,-52,-35,-2,-75,-72,76,-115,-56,12,-20,34,-61,23,-8,90,105,-90,-102,105,-90,-100,-109,28,19,-66,21,121,-122,96,-68,58,40,-33,8,91,-115,-64,-102,-123,-73,5,23,-12,62,68,-43,69,30,8,3,-24,-104,15,41,-82,67,-19,-59,-2,-128,-64,24,0,13,8,-38,-36,-100,-101,-119,-60,-90,97,-95,52,12,99,71,8,-15,-99,25,-111,-78,-40,-20,-83,42,-61,19,17,29,-109,-47,23,-115,68,-29,86,105,27,79,-98,-44,-93,-107,-128,-13,-51,-100,-24,48,-68,-111,41,-17,-48,-2,-18,-78,108,12,-54,54,-116,-72,38,-15,3,86,-90,-105,-58,-51,-113,-89,119,84,121,21,124,38,-119,52,70,89,-124,77,-57,-90,116,14,71,-23,25,115,70,44,119,-102,-121,-26,117,6,-128,-107,56,-124,-101,-26,-69,-120,-27,-11,10,-48,44,2,-50,52,-12,-53,-62,101,51,-82,6,17,18,-17,-95,-5,60,44,87,60,-80,90,114,53,-127,117,8,122,84,-39,40,90,-113,114,-125,-20,106,78,97,-39,-59,119,-5,124,-24,94,-62,-71,48,-96,72,27,-76,119,37,46,-18,-66,81,68,73,114,-18,-29,-38,-76,69,60,-20,94,72,49,-63,13,-93,29,-35,80,92,13,-68,-65,-113,72,15,119,29,-121,103,88,-123,6,0,-44,3,-50,-14,57,43,-57,83,-59,-20,76,78,-36,40,19,7,105,-118,17,-101,-20,7,100,20,12,68,23,37,19,83,27,79,-89,3,56,43,45,123,-82,-66,-62,-13,-56,56,-38,-25,-29,52,-38,-98,-63,-12,65,-63,-43,-53,-39,93,-70,-87,74,80,-28,-103,-7,-12,-113,-84,-109,5,-97,37,-80,24,28,-90,96,-48,-89,51,-77,54,23,-99,-36,-7,-10,-21,39,60,-2,-109,-14,127,0,30,103,-27,54,41,-104,-57,56,-126,57,-16,40,57,84,-4,16,11,94,2,23,59,-35,-31,-42,90,46,31,65,-1,0,126,61,96,120,93,21,-123,-51,-61,123,69,-126,-60,-54,46,94,-124,-118,-120,93,27,-80,-32,-15,-24,30,39,-112,8,31,35,-44,-4,68,-105,-60,-44,-31,-72,118,47,7,64,-93,38,123,15,96,3,-48,-21,-105,35,-53,-113,-9,-23,-95,55,-72,105,22,-38,97,98,110,30,-89,-86,-11,-50,-126,37,32,-63,-128,-74,-81,33,-8,-2,-70,-4,127,93,126,63,-82,-65,31,-41,77,-73,127,-14,-1,0,-109,95,-99,-41,-25,117,-7,-35,83,24,2,50,-21,92,127,-67,74,115,99,9,38,87,-112,-52,-73,-89,-17,-56,-47,23,-122,-4,-23,-128,-115,-52,-110,27,-103,105,-105,38,-10,-2,-124,-35,-113,-78,-33,-45,-103,49,-72,123,-63,-119,-128,-108,112,76,-11,-124,118,117,-118,90,-88,81,-85,15,68,-119,-107,-101,-127,-39,-4,-37,-47,38,-4,55,-115,-7,-63,-17,-81,8,-22,-25,102,-61,-109,23,38,60,117,9,-126,-102,-88,-47,70,62,-120,-104,0,95,58,12,-95,-61,-85,-8,76,10,-81,109,121,55,-115,114,-38,111,-127,-70,-85,-123,61,97,12,59,35,-54,-14,-29,-30,-6,28,-12,21,-60,49,81,-126,-117,-110,60,-122,-71,-33,103,-105,-12,27,1,-128,0,-12,4,-1,0,44,8,1,103,59,-21,45,-12,81,-94,37,58,-108,20,28,-92,107,-7,61,-70,-8,59,61,-32,-114,75,-125,-22,100,116,78,-123,73,-22,59,99,5,-20,86,-69,-82,-114,-124,101,90,34,27,-116,-64,-119,51,116,13,-14,-72,-65,4,-82,39,109,-116,-25,123,-42,19,-11,-20,104,-103,-53,110,52,93,100,19,-78,-126,-9,-13,10,-61,23,84,-67,-97,-30,126,79,126,-119,92,-89,43,88,-25,-94,121,109,55,-58,91,97,25,90,9,92,60,-117,-102,46,89,85,87,110,0,39,120,0,-63,35,-113,101,91,119,-103,-41,98,71,75,107,-30,31,26,-27,69,6,9,87,-74,80,-9,-12,28,63,11,-63,97,-18,-14,-127,-26,108,-51,57,52,-119,-50,26,6,109,13,-83,-123,113,49,29,59,89,88,-46,-31,54,125,29,-82,-125,-124,8,114,92,-40,-51,-59,79,121,-78,72,42,102,-58,104,-30,101,-118,67,29,72,54,-38,-128,-123,89,112,7,67,-95,119,117,88,46,-8,-60,-57,111,-82,-103,74,-28,-44,-16,125,-76,-103,5,33,76,82,101,-71,-10,58,-96,-113,-107,-84,-58,57,-104,-106,10,92,-55,-82,71,-55,-63,-2,-60,-56,-104,68,77,80,50,91,-96,-116,25,48,-89,64,13,16,41,-110,106,-85,-70,-6,0,75,25,84,64,41,-14,-63,107,-126,-62,82,40,-116,35,48,11,57,-14,24,-46,13,-74,-44,4,42,-53,-128,52,73,58,98,-74,-83,-5,98,83,106,49,-95,-25,-24,71,74,108,-66,90,26,102,92,80,125,-111,-103,-77,113,38,-103,66,65,-97,88,-91,78,-41,106,-51,-33,-31,105,-90,-102,105,-90,-102,-92,-2,-103,88,-103,114,-9,52,79,115,119,43,-79,-54,-10,53,94,109,14,63,-67,-53,-27,116,-94,107,-19,-66,95,-30,93,21,-1,-38,0,8,1,1,0,0,0,16,-65,-27,57,-81,75,-55,-100,-57,-8,-9,-121,-72,31,-95,-52,31,-49,3,-16,-41,14,-4,119,-28,-92,97,-63,-22,59,47,-11,-1,0,-1,0,-1,-60,0,37,16,1,1,1,0,2,2,2,2,3,0,3,1,0,0,0,0,1,17,33,0,49,16,65,32,81,97,113,48,-111,-16,-127,-95,-47,-63,-1,-38,0,8,1,1,0,1,63,16,55,-120,106,56,-126,10,-49,85,65,-10,20,-116,-117,73,17,-46,-43,69,48,-33,-31,-61,12,48,-61,12,48,14,-77,-126,34,17,-56,117,56,8,-122,93,46,32,-119,15,89,-119,-23,68,69,23,52,-37,-7,41,-28,-77,2,-127,75,47,7,53,-99,45,-102,19,-96,-118,-120,21,5,32,49,-79,-120,0,8,0,4,0,0,1,-86,-31,44,-112,-56,44,69,11,66,-33,59,86,28,-6,4,69,80,69,6,30,10,42,94,47,15,10,4,68,-94,35,-16,58,-119,-3,28,123,-120,-3,-100,18,-93,31,-57,42,-101,-99,-94,-53,-46,16,-65,-111,-14,-128,118,75,41,101,14,85,-49,-49,-120,51,-66,43,59,61,-15,-9,-48,-76,57,34,-43,104,-20,-100,24,-83,65,116,-43,120,-64,34,11,80,69,-2,-72,-14,40,106,-5,-56,4,68,98,34,120,-19,34,-36,2,59,-101,110,-80,-55,-68,-59,55,32,-46,-118,69,20,49,-121,-111,49,33,75,68,-43,-110,-107,-116,0,12,27,-31,74,38,-120,3,116,72,123,-28,31,-25,121,-62,-20,10,-88,0,30,72,66,113,-14,112,34,-105,-118,1,-52,-6,57,-54,-99,-121,15,70,34,-124,77,13,-108,-60,-34,-82,103,1,-97,123,99,73,102,18,-90,-88,23,27,-9,43,-51,-51,3,43,-127,3,66,54,0,68,6,-37,12,-103,-11,107,83,55,0,104,124,25,-7,-100,58,-118,30,-70,-97,-38,126,124,117,-128,122,61,-62,-10,63,-8,-111,7,-106,-58,-122,-123,-15,2,-115,-47,115,113,-30,7,33,107,73,-85,7,49,116,32,14,100,-52,-87,-71,-61,-6,-110,15,-95,22,-17,-44,-98,-25,21,67,21,73,-126,-13,2,-56,64,32,40,12,40,2,20,81,92,-34,-24,9,67,46,-125,10,-25,106,-86,-86,-81,-117,30,53,-98,-56,67,-95,32,20,-5,-66,72,73,-34,64,5,13,-118,-123,-6,79,89,-16,-128,-23,115,-75,11,-52,114,-45,115,-32,-122,-48,117,-59,44,66,-86,43,-75,64,32,-16,73,-117,-56,83,-71,25,-43,-54,-4,-100,8,124,-18,3,31,104,-38,-63,66,58,-112,42,115,-119,0,92,-110,28,-52,22,-71,82,-81,-32,53,16,-35,10,51,81,-4,-30,103,87,-21,-26,112,34,3,87,-120,61,-108,3,107,-13,-13,6,64,105,-27,60,10,93,5,97,127,-66,72,4,-44,-80,16,43,-16,-89,122,116,97,79,-29,22,-3,121,-128,-126,-78,-11,28,4,-5,-128,23,54,57,125,79,104,-56,40,2,-22,32,-78,-70,-65,6,17,65,-124,-110,64,-99,-11,-23,-11,-16,10,-50,6,64,12,-106,0,-76,101,-19,-76,-66,32,94,73,-26,54,24,106,12,70,7,-60,34,34,76,72,108,81,-24,34,17,32,-97,99,-5,-28,-3,-65,-33,-41,39,-19,-2,-2,-71,63,111,-9,-11,-56,52,57,100,-88,-64,-22,43,-128,-105,7,-97,-116,98,-90,112,-60,-55,84,-120,67,-59,86,63,-5,-105,-73,-44,-102,-39,27,97,-89,-62,-37,100,89,125,-63,-118,-13,-44,-61,22,-127,-91,38,-108,-63,-5,77,62,49,4,11,1,5,-121,81,-82,-110,39,-104,110,55,-29,-99,-29,-29,-85,68,1,85,96,7,-41,-61,1,74,-80,123,-105,-40,43,25,-17,-6,-7,-36,75,45,-102,-105,-95,-45,81,-90,-3,113,50,-17,-46,-67,-127,-45,-61,-20,80,121,-121,-21,63,61,86,-124,98,12,68,-5,-30,-43,126,-4,-86,105,87,81,64,95,-58,-14,59,-97,32,95,-75,-109,87,87,-38,-66,64,60,83,44,-64,6,-86,-28,57,-82,-58,-16,69,5,-99,-126,86,31,-64,72,119,-86,-111,80,36,-102,4,-19,53,-49,97,-1,0,62,59,-30,-5,50,-62,81,-87,85,116,2,-52,124,-8,116,-64,0,4,0,1,0,0,0,-13,14,73,-1,0,121,78,-58,53,41,103,103,59,-103,91,84,-88,70,34,81,79,-81,46,96,10,-64,70,35,-46,19,-5,38,-8,-17,-121,-104,-62,-95,17,86,76,-118,-80,2,-12,112,69,28,65,76,-128,35,20,-96,-91,74,104,42,-92,6,9,17,29,99,55,-128,-111,7,62,108,107,-95,116,-110,-123,-60,67,123,60,31,-7,-25,47,113,-9,46,50,-45,59,16,-24,28,-116,84,84,-46,72,-15,-52,119,-1,0,-72,29,-114,-77,26,107,-41,-120,110,30,106,-17,-12,1,85,112,5,112,-28,-80,-41,-114,-89,66,11,71,95,89,52,79,-113,-24,45,-123,66,66,11,-58,52,-44,-108,90,34,27,48,-81,108,-55,26,93,-112,97,4,67,0,-96,64,0,76,-29,-114,-6,118,-25,89,3,89,-10,14,-45,-31,110,2,-19,34,91,-39,81,-86,17,73,-121,-106,-87,19,85,-51,27,4,-89,109,73,-57,-64,-122,69,-47,19,68,-126,92,-127,-21,26,97,35,-17,121,-59,73,-124,90,112,-78,89,15,-116,-117,65,105,-116,108,116,-66,-27,-55,98,-64,10,-32,7,71,-122,115,-64,49,-22,-96,80,0,101,-19,77,-27,-62,109,66,69,-83,4,-106,-45,-21,-108,73,-82,-86,-59,33,0,32,13,79,36,60,-117,-72,81,23,7,86,-58,-116,94,39,84,30,17,8,-119,68,4,-108,2,34,-9,-85,85,105,98,81,8,-24,-89,-65,13,-102,11,47,124,-68,-88,85,86,-86,-81,-63,31,56,84,-60,-122,22,16,36,33,-27,-14,-22,89,-44,0,-70,76,-75,-75,-25,-42,59,-105,-91,-117,0,43,-128,29,28,59,-72,60,88,38,81,97,-96,35,-70,40,-85,72,59,19,46,-99,103,-29,-124,23,-39,58,66,-67,-107,48,-126,-124,-112,6,64,-93,-46,-46,106,18,37,-115,63,-125,12,48,-61,12,48,-61,106,-4,-91,69,-45,-43,-108,-116,72,-126,63,68,-88,-56,20,-20,76,-53,47,-93,-125,-60,-112,-59,40,-123,76,44,24,-68,-126,2,68,-90,-6,-30,91,111,-15,106,66,-78,51,-61,-1,-39,};
}

//the numbered cards from 0-9
class ImagesNumbers{
	public static final byte [] uno9 = {-1,-40,-1,-32,0,16,74,70,73,70,0,1,1,2,0,37,0,37,0,0,-1,-37,0,67,0,27,18,20,23,20,17,27,23,22,23,30,28,27,32,40,66,43,40,37,37,40,81,58,61,48,66,96,85,101,100,95,85,93,91,106,120,-103,-127,106,113,-112,115,91,93,-123,-75,-122,-112,-98,-93,-85,-83,-85,103,-128,-68,-55,-70,-90,-57,-103,-88,-85,-92,-1,-62,0,11,8,0,-102,0,100,1,1,17,0,-1,-60,0,26,0,0,3,1,1,1,1,0,0,0,0,0,0,0,0,0,0,0,3,5,4,2,1,6,-1,-38,0,8,1,1,0,0,0,1,-17,122,-64,0,-21,-56,-105,-95,55,50,-64,15,108,108,92,26,30,-53,52,108,-32,57,-82,-81,-97,-83,-101,13,37,-31,6,-38,122,-96,108,70,-84,-88,0,-81,-67,95,60,81,-52,-74,-89,-128,-95,89,95,61,-91,-87,-74,-106,-62,81,66,-78,-66,126,-100,-70,-35,71,-76,-103,101,106,10,-106,-108,84,-41,30,-54,-94,23,-34,-84,82,-50,-22,-77,-44,-56,117,109,94,72,-62,108,-41,62,-52,-68,116,-8,-84,124,-6,78,-86,-73,36,-26,109,-30,-57,31,59,-31,-77,88,78,-33,51,101,-123,124,-16,0,80,70,109,-42,60,-7,-34,0,54,116,-82,52,88,-14,78,0,40,-81,29,124,-98,-40,84,-33,80,-26,-30,67,43,-27,-30,-57,63,60,57,60,1,-71,90,-23,103,-17,55,-128,1,-34,-87,120,126,-117,-96,0,9,-78,-1,0,-1,-60,0,37,16,0,2,2,1,3,4,3,1,1,1,0,0,0,0,0,0,2,3,0,1,4,17,18,19,16,20,33,51,32,34,49,35,48,50,-1,-38,0,8,1,1,0,1,5,2,66,107,108,-28,9,-56,19,-112,39,32,78,64,-100,-127,57,2,114,4,-94,27,-124,20,116,-43,-20,56,-42,89,-112,-29,-8,48,-80,-65,-106,-70,76,102,111,24,-49,88,120,60,-127,35,-68,-118,-47,93,22,-110,57,-79,75,-82,112,-85,-18,0,-95,36,14,-79,124,50,51,-41,95,-70,4,-56,109,23,69,38,-124,89,-111,119,-16,89,-40,16,-43,89,70,122,-21,-59,-13,4,97,-47,-59,-82,-106,44,109,-77,-27,-120,90,-124,103,-81,-94,23,66,46,103,33,8,17,78,6,66,27,31,-122,37,-3,-93,61,113,11,-34,89,12,-42,-46,-66,66,-86,-96,-82,101,-22,67,69,76,14,50,-23,-119,95,104,-49,93,121,-123,-4,19,49,43,-23,-108,90,4,-58,45,87,-105,94,58,98,87,-46,51,-41,-116,26,-109,-113,121,-52,66,-16,-48,-28,14,19,-43,65,-80,50,107,-7,116,77,109,92,103,-82,-1,0,-106,63,65,43,2,12,-111,-70,-18,23,20,118,-55,-106,90,4,80,-18,61,-33,-38,126,-52,-94,-5,116,-57,16,43,102,56,-19,-92,-99,-38,-125,96,100,-98,-29,-104,-11,-80,49,-81,87,116,109,-22,-50,-107,118,54,25,35,117,-50,-72,-36,-99,122,44,119,-106,73,105,88,-98,-56,87,-96,-33,-17,68,88,84,-34,-119,-67,19,122,35,116,-74,5,112,-88,-81,117,-30,123,35,63,-29,-25,-114,-88,-26,-17,41,-119,-20,-24,117,-95,124,80,-99,-47,-20,-42,112,50,18,-120,43,19,-39,47,-60,-55,15,63,5,35,72,-41,-21,43,-9,-62,-127,-6,105,-119,-20,-116,-11,-91,-34,11,27,89,-64,-56,56,-41,53,82,99,26,71,-47,126,-57,-99,5,52,68,-107,-119,-20,-105,90,-47,14,-46,-94,42,-100,-20,-106,101,127,26,112,-112,-75,-101,-85,16,58,37,-101,-60,-42,39,59,80,-99,-88,78,-44,39,106,19,-75,9,-38,-124,-19,66,118,-95,7,24,42,-1,0,35,95,123,-58,-18,-82,-65,63,-57,46,-17,-89,-1,-60,0,37,16,0,2,1,3,2,7,1,1,1,0,0,0,0,0,0,0,0,1,17,2,16,49,18,33,3,32,34,50,65,81,97,19,48,51,-1,-38,0,8,1,1,0,6,63,2,-43,86,109,-36,-114,-28,119,35,-71,29,-56,-18,71,114,59,-111,-77,55,68,94,106,112,67,-2,16,-14,-84,-60,39,78,-24,-90,115,-55,-44,-27,-99,52,29,84,19,-61,29,-86,55,20,87,11,-47,-91,91,95,16,-118,54,92,-101,31,-94,-75,86,-1,0,52,40,-90,13,117,-97,57,-93,-43,-86,-65,-23,89,-16,-39,24,55,92,-115,90,-85,124,70,-107,-125,-31,-23,17,38,-28,93,-69,85,104,-14,-20,-39,30,-18,-99,-37,-75,70,-89,-30,-18,-110,8,-126,7,116,-84,-49,-82,-14,-114,-83,-103,-111,-65,4,123,-78,70,-97,74,-6,125,93,-86,-114,-107,-71,16,65,11,-59,-97,17,-115,-35,-34,81,-43,-79,-110,40,-76,11,-122,-82,-33,35,-42,120,60,30,14,-100,26,-98,73,119,127,-61,93,88,62,114,-75,-51,53,96,-47,78,12,18,-41,38,-75,-25,-105,85,102,-102,118,86,-21,108,-102,106,-107,-22,-11,26,107,-63,52,57,48,117,56,54,-35,-101,-39,10,105,-109,-12,74,47,4,27,51,38,-17,-106,56,-117,6,-102,118,-92,117,91,-23,-70,60,-103,102,89,-106,101,-103,102,89,-106,123,-73,67,-40,-37,-7,-85,127,-1,-60,0,34,16,1,0,1,4,1,4,3,1,0,0,0,0,0,0,0,0,1,0,16,17,33,49,65,81,97,113,-95,32,48,-111,-127,-1,-38,0,8,1,1,0,1,63,33,56,11,-87,112,58,125,32,0,0,0,26,85,-106,-91,-24,-52,5,56,-114,8,-65,45,-72,34,36,55,-24,-106,9,-15,-75,-11,17,54,65,43,-115,-94,38,88,88,118,79,86,49,46,-81,63,-119,-28,-23,-93,-102,-24,-53,29,88,99,40,106,-103,113,-126,-46,-29,-1,0,-112,-94,113,-118,122,-110,-48,89,114,23,95,-100,78,101,14,105,-24,-60,98,2,-33,117,-66,-106,57,-125,-105,101,61,72,-63,82,-10,-30,-107,-128,126,51,-101,124,17,-84,-74,-32,124,-82,55,109,79,82,-67,0,106,-15,110,104,106,51,102,105,87,-52,124,27,-68,41,-22,81,111,-7,38,123,-61,115,11,-61,114,-59,44,20,-115,-114,4,-114,-85,-7,86,-19,10,122,-112,40,14,98,-80,105,23,81,122,-12,-39,-8,-60,-11,-70,-105,80,-76,-11,33,62,-119,-68,-68,24,40,93,102,-27,-13,-105,19,-105,-63,46,92,-62,89,-30,-67,86,-89,-85,44,-73,84,-58,-30,-124,8,17,-58,58,-110,-61,118,-44,-9,-28,46,-75,-86,8,11,50,-15,26,-85,58,47,17,79,20,100,-116,2,-3,-62,-68,98,-127,-2,38,93,-82,74,-78,-34,-75,50,-106,72,35,101,45,-125,-5,81,68,-63,-42,77,-51,-34,41,-38,2,43,-90,-70,-13,-76,-19,-2,39,111,-15,59,127,-120,-100,-8,-95,-74,-4,85,-40,-51,-34,40,47,111,-89,-47,-40,-122,-93,90,53,-43,55,120,-93,-110,-45,-79,-113,-54,-25,81,-9,46,-18,-29,-46,-105,20,68,-35,-30,-120,10,-24,-104,-39,-49,-128,47,-87,-46,103,72,-41,69,10,3,114,-63,92,-67,-27,-31,114,117,77,-34,41,-22,64,54,-8,51,81,-118,87,-28,-124,-50,5,-71,-79,99,-95,79,118,111,-125,7,-35,122,77,-34,41,123,92,-58,69,-60,-34,-91,46,-59,126,33,90,-11,65,0,-79,44,8,111,84,3,111,-35,57,42,119,62,-128,0,0,0,92,-39,-14,-104,29,8,116,96,64,42,-76,-47,-11,16,35,97,-89,-1,-38,0,8,1,1,0,0,0,16,0,8,6,-100,28,-78,65,65,9,0,-50,3,34,106,6,4,-109,-28,15,-56,116,64,96,4,8,-92,48,-128,71,-121,-11,-128,47,-1,-60,0,39,16,1,0,2,1,4,1,4,1,5,1,0,0,0,0,0,0,1,0,17,33,16,49,65,81,-15,32,97,113,-95,-79,48,-127,-111,-63,-16,-47,-1,-38,0,8,1,1,0,1,63,16,-35,22,1,-30,57,36,7,114,-81,-5,79,53,60,-44,-13,83,-51,79,53,60,-44,-13,81,-124,-86,-23,-107,76,56,122,-117,16,-27,87,17,14,-20,5,-64,-109,54,-126,62,55,121,-69,27,-42,77,-98,19,-46,37,65,126,38,-32,31,36,-70,36,116,-59,-93,-79,-39,22,88,63,36,-61,-27,-59,46,-127,86,95,34,-108,91,103,-72,-40,69,-127,-43,-112,-93,-10,8,122,-90,-25,-60,89,105,109,125,-62,-109,-84,66,-29,51,-40,91,-34,13,-22,16,-114,-97,123,19,116,46,78,-55,112,-26,91,-53,43,115,-69,123,48,21,-96,-75,-104,2,1,98,-30,112,-29,-81,118,34,-76,-81,-66,-93,-104,39,14,-56,116,54,-38,-25,79,-67,-122,-80,-85,87,58,67,-31,-125,-7,76,3,-28,-15,-23,82,-31,-29,-43,96,-39,-64,116,105,-9,-70,-68,26,50,31,-108,-87,-81,0,79,-107,51,-119,-2,-20,-90,124,-87,-24,54,29,89,-89,-34,-24,9,97,-83,51,108,-40,-23,-53,26,-21,89,20,58,0,111,46,-59,-69,-81,104,-119,-120,-59,-54,-93,77,-41,102,-82,-61,-86,52,-5,-40,37,90,-88,-122,-116,119,-45,-120,-86,-38,-38,-64,0,-54,-117,16,64,-37,-53,-19,-96,50,-54,-14,121,-105,-124,48,-37,-67,69,83,97,122,125,-20,-38,2,-68,-9,20,-9,-15,26,92,84,6,-103,-34,24,52,28,-89,-71,-35,93,-15,9,59,-77,-14,-128,-123,-88,77,2,-38,37,-15,-62,-35,5,-119,-34,84,3,6,127,125,88,26,-127,-69,33,-66,48,-59,-120,-44,-32,55,-114,-44,3,79,50,-127,-93,-112,-20,-47,-114,44,27,-8,67,-67,28,-81,-99,28,-107,-114,-16,-38,97,-5,-44,-102,56,119,49,68,-28,-50,-15,40,36,-35,118,-104,13,97,107,-34,9,-79,-113,-9,-45,26,-106,127,8,-37,-39,-99,81,75,112,-42,-32,-62,-58,20,88,76,-68,79,-9,37,72,65,-62,-1,0,-88,-86,-38,-38,-63,-43,109,-53,-47,49,8,27,53,112,122,-23,28,79,112,27,-41,29,-85,-84,-117,-12,0,0,57,75,108,8,92,8,-29,-65,-120,-111,42,93,-22,-20,49,106,-15,18,-102,125,100,3,-103,1,-4,-58,-51,75,-127,-49,-65,-95,-51,-67,-118,-105,-74,108,-11,41,22,-21,48,67,75,23,-71,-126,-126,98,-5,-62,-42,-122,-82,-11,117,110,-126,-42,5,-121,46,-72,-12,34,-96,-81,-76,16,40,37,-113,-5,-128,-116,28,89,-117,-106,32,-89,17,-21,39,-94,-37,121,11,38,-81,-9,-77,127,-55,72,93,68,-77,14,105,103,-5,-79,-125,-108,-117,-52,73,-79,43,-33,49,122,-96,-19,-79,-90,31,14,12,21,-104,30,32,2,14,28,-22,-28,18,-64,-112,-65,-91,79,-91,-90,97,-81,-58,29,67,93,-46,-59,-74,-35,69,27,26,72,-70,-47,-127,-52,-29,-34,78,88,-43,-74,-51,14,65,92,9,-78,-21,-66,99,119,-9,79,49,60,-60,-13,19,-52,79,49,60,-60,-13,16,3,40,-29,100,10,-32,23,-20,16,-22,-104,44,-27,-114,-27,89,-77,29,-69,-47,-6,78,-114,-59,-125,-66,-97,-1,-39,};
	public static final byte [] uno8 = {-1,-40,-1,-32,0,16,74,70,73,70,0,1,1,2,0,37,0,37,0,0,-1,-37,0,67,0,40,28,30,35,30,25,40,35,33,35,45,43,40,48,60,100,65,60,55,55,60,123,88,93,73,100,-111,-128,-103,-106,-113,-128,-116,-118,-96,-76,-26,-61,-96,-86,-38,-83,-118,-116,-56,-1,-53,-38,-18,-11,-1,-1,-1,-101,-63,-1,-1,-1,-6,-1,-26,-3,-1,-8,-1,-62,0,11,8,0,-102,0,100,1,1,17,0,-1,-60,0,25,0,0,3,1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,3,4,5,2,1,-1,-38,0,8,1,1,0,0,0,1,44,-32,0,14,-68,-52,-44,-51,-19,75,0,61,-48,119,89,118,46,33,-44,114,122,88,102,-73,-72,107,-30,96,102,-97,70,67,-43,84,-85,0,-47,-96,-57,-14,-60,40,0,-74,-61,29,-84,-102,-42,-82,31,11,108,50,44,-122,-86,51,116,-111,33,125,68,11,75,-76,36,-81,61,38,-85,9,96,42,-74,90,-94,-107,-102,93,-112,-56,105,113,5,-51,-52,-73,-38,-52,-91,-108,104,33,-16,-94,-114,-84,49,-4,14,-5,-29,-117,-94,-78,-61,31,-64,2,-75,38,-37,12,-98,0,40,-18,126,108,-80,-122,64,43,-15,84,-5,-51,-124,62,-91,-116,-103,93,-34,-65,44,50,78,-8,-32,61,-71,45,-83,61,-92,0,15,104,-50,78,-72,0,1,36,63,-1,-60,0,37,16,0,2,2,1,4,2,3,0,3,1,0,0,0,0,0,0,1,2,0,3,18,16,17,19,49,4,33,32,34,50,48,51,65,35,-1,-38,0,8,1,1,0,1,5,2,-86,-67,50,89,-110,-52,-106,100,-77,37,-103,44,-55,102,75,55,6,50,-122,-114,-72,-98,-125,-79,102,20,-104,-54,87,-8,42,108,-127,0,-61,-46,-99,-98,-60,44,110,-12,-70,45,101,-90,53,-92,-27,2,114,41,45,88,105,70,-121,-86,-1,0,-78,-52,-14,-73,-6,-30,-41,-76,123,119,-8,35,98,70,-38,30,-65,-34,102,-116,-59,-54,-88,-84,59,-106,63,26,79,-42,30,-113,114,-92,-38,59,100,126,94,62,-121,-93,-35,107,-111,-75,-12,90,68,-39,35,82,33,27,29,60,125,15,95,-23,-1,0,-107,114,-127,-10,-76,-20,-78,-93,-70,-34,61,-23,64,-6,-61,-43,75,-69,-40,-39,52,-83,-79,98,3,-114,3,-72,1,69,-83,-109,105,88,-39,33,-21,-15,94,-107,38,80,-112,-93,-98,2,24,90,-101,104,-125,119,7,-34,-105,-97,122,83,-8,-68,122,-108,117,111,-30,87,-11,74,78,-25,75,61,-66,-107,62,39,-79,-62,-69,-6,81,107,-27,17,114,107,90,120,-6,-98,-11,12,68,-27,104,88,-99,20,113,-95,-9,60,125,15,71,-65,-107,73,44,124,-116,-15,-11,111,77,-15,-82,-67,-27,-106,76,76,-37,105,-29,-21,114,-4,86,-88,-10,74,-58,-17,101,-123,77,-97,106,-4,125,15,73,100,106,-73,-100,77,5,38,125,43,-114,-27,-76,95,-45,-78,-119,119,-25,-57,-43,-122,-52,24,-119,-54,-45,35,-16,30,-114,-24,-14,-41,-34,80,61,74,-37,37,101,13,56,86,112,-84,-31,89,-62,-77,-123,103,10,-50,21,-100,43,5,42,39,82,-57,-35,-121,127,-59,121,-45,-1,-60,0,34,16,0,1,4,2,3,0,2,3,0,0,0,0,0,0,0,0,0,1,16,17,49,32,33,2,18,48,34,65,81,97,113,-1,-38,0,8,1,1,0,6,63,2,-107,107,44,-78,-53,44,-78,-53,55,-122,-51,-8,111,13,40,-119,-122,-51,113,55,-60,-98,34,-31,-93,118,-45,-56,-114,56,-50,93,-71,122,118,95,5,-57,-86,55,-56,-115,26,-63,112,-3,-66,-101,100,-27,62,72,-1,0,-41,-107,106,105,70,66,30,50,87,-20,-89,39,92,-31,8,58,-96,-71,-39,102,-43,-89,-19,-105,-53,-78,-70,-70,-27,43,71,94,37,50,-65,108,103,-111,28,68,33,9,21,-6,-14,-94,120,-108,108,-4,-85,-24,-7,9,20,46,22,89,120,-20,-124,-94,95,126,122,-12,70,-1,-60,0,34,16,1,0,2,1,5,1,0,3,1,1,0,0,0,0,0,0,1,0,17,49,16,33,65,81,97,32,48,113,-111,-127,-95,-1,-38,0,8,1,1,0,1,63,33,42,-2,115,19,-55,60,-109,-55,60,-109,-55,60,-109,-55,60,-112,-60,16,10,19,99,-128,81,-63,20,3,-73,17,5,-96,99,-75,-14,-83,5,48,-57,-89,34,101,11,-103,33,10,-9,3,52,77,-53,-98,-11,-14,-55,-56,-37,45,64,-38,40,21,-65,-28,20,-105,76,-110,-107,-66,-25,117,94,76,50,11,104,-124,127,-25,19,-128,69,-73,125,84,-45,16,-83,-68,-23,-110,93,92,-18,4,113,55,103,-14,115,18,88,-100,125,93,94,-76,-55,50,104,3,-7,-57,-65,-113,-62,100,-103,38,95,4,-77,-92,-127,109,18,-91,-27,-94,32,-29,18,-121,63,38,73,87,95,98,-40,-27,-91,-119,-22,63,-69,71,-13,74,15,109,104,79,122,100,-106,28,9,-6,-125,77,-27,-60,-88,-28,-99,-126,-91,108,-63,48,-40,53,-96,105,-110,59,-11,-99,70,-72,9,123,118,37,47,-92,-67,-104,-121,-44,-46,-95,45,99,-115,118,124,53,-91,106,34,83,26,23,-62,103,-48,-115,43,-121,122,-83,70,-52,-31,-101,119,12,119,16,58,4,53,-64,68,8,32,56,-77,-88,-31,-114,-37,-15,-126,95,11,-64,119,-108,74,-73,83,36,-55,-9,-6,-126,126,-84,-7,13,47,-70,54,-72,32,-90,-54,43,34,-66,13,-22,99,-32,45,-38,84,95,68,-68,-60,64,35,0,-123,74,108,-23,-44,-55,3,120,-121,115,-94,118,117,47,12,21,-33,29,105,122,114,-118,-128,-74,37,101,-69,81,-60,-80,38,9,104,-105,-54,-8,84,50,-120,-70,72,21,-58,-120,46,-38,31,-95,48,-55,-20,-49,102,123,51,-39,-98,-52,-10,103,-77,61,-104,-123,-26,108,58,34,-18,104,35,65,76,48,126,38,10,58,127,-1,-38,0,8,1,1,0,0,0,16,-65,-57,6,-64,6,102,17,64,-128,4,22,39,-29,37,-87,21,-30,71,-116,-44,40,-128,38,4,56,-119,48,8,-96,-8,10,127,-49,-1,-60,0,36,16,1,0,2,2,2,1,5,0,3,1,0,0,0,0,0,0,1,0,17,33,49,16,65,81,32,97,113,-95,-15,48,-127,-111,-16,-1,-38,0,8,1,1,0,1,63,16,81,43,-67,24,-96,-54,4,-3,-87,-5,83,-10,-89,-19,79,-38,-97,-75,63,106,126,-44,-33,79,-18,101,99,-61,-30,58,-67,108,-8,-128,6,2,55,12,-88,18,-30,86,-122,83,76,58,77,122,68,-24,95,-120,-119,-79,62,98,-74,-125,-19,28,37,-10,-59,-20,-41,-68,-6,12,6,44,-118,71,3,87,22,72,-114,78,96,-9,25,-128,80,-53,-64,-21,-52,61,64,-13,29,-102,-4,35,16,-92,-21,-113,-96,-62,-120,9,109,-59,-90,14,-96,13,-82,-97,-20,66,11,94,-91,-65,10,-55,31,-22,31,113,17,74,-67,-68,-92,14,89,34,30,30,120,-6,12,83,37,38,-48,1,74,-69,-124,54,-16,37,-32,44,96,-15,16,-91,116,61,89,-13,-107,-57,-48,103,-40,120,66,52,5,-38,59,-77,93,31,-62,-3,6,125,-122,99,109,-71,97,81,43,27,93,-60,32,-75,-22,24,-40,-8,79,26,-38,-9,-105,-27,111,30,99,35,-95,-23,126,-125,20,-61,109,32,1,-7,-30,-86,-82,-40,104,-45,73,127,84,-86,-71,109,-35,-25,-52,-65,-85,85,92,48,-70,103,-100,75,74,-33,31,65,-105,-34,-42,58,-4,7,6,91,24,101,-75,-109,76,-7,86,104,96,90,-63,-78,-6,-114,55,21,-115,-43,-15,-12,24,69,-84,-109,124,48,43,-48,121,-104,48,26,9,-85,127,116,-40,5,-78,84,-99,44,39,15,81,-35,-80,43,-108,121,100,46,5,-41,61,2,-5,-105,73,71,39,1,70,-20,-30,48,-66,-83,-41,4,102,93,68,119,-75,-14,-86,124,-41,63,-127,-45,-38,2,39,96,120,-108,18,-114,16,107,-40,-7,-128,-113,-19,-102,-101,-28,93,49,-47,17,45,-85,-24,-83,-88,29,92,-1,0,-79,17,-56,-5,64,86,-115,-54,9,-85,17,75,90,-74,-14,-3,6,125,-121,-42,15,94,-127,-119,65,-30,61,10,88,-113,115,24,85,62,-92,122,-49,-36,7,64,-31,78,-27,-71,71,-60,106,-103,123,-14,-22,97,101,91,125,8,0,85,-24,-127,66,-39,80,-85,-23,-62,-7,-103,-119,46,-27,73,-122,-15,-71,70,-22,103,-105,-24,48,14,69,-102,94,-96,-108,81,-51,79,-5,48,43,19,11,-48,100,-54,-21,-63,27,85,108,48,21,-41,-30,85,3,-85,-108,90,60,-111,-101,-68,50,-74,-96,117,124,32,-47,67,-17,55,-54,-125,-79,-72,-97,-101,87,6,-128,115,20,105,-64,-117,122,-101,-102,69,124,-1,0,12,68,68,68,69,-43,126,-58,5,2,-126,61,-73,3,14,-30,-77,13,-11,51,-8,-113,-30,8,64,119,92,127,-1,-39,};
	public static final byte [] uno7 = {-1,-40,-1,-32,0,16,74,70,73,70,0,1,1,2,0,37,0,37,0,0,-1,-37,0,67,0,80,55,60,70,60,50,80,70,65,70,90,85,80,95,120,-56,-126,120,110,110,120,-11,-81,-71,-111,-56,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-62,0,11,8,0,-103,0,100,1,1,17,0,-1,-60,0,24,0,0,3,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,2,3,1,4,-1,-38,0,8,1,1,0,0,0,1,-53,-32,0,1,-51,121,25,48,52,47,89,-13,-66,-56,122,102,-103,121,-13,-38,57,108,-98,13,-46,-45,-25,100,-78,32,7,69,103,-49,-106,68,0,45,121,-13,-77,-58,-6,16,-53,94,124,-9,-122,84,119,-27,-53,-38,114,38,27,-43,-50,-121,91,79,33,-127,103,-26,27,-95,-47,32,7,92,100,95,46,-111,-104,111,103,52,-38,-103,-48,-100,-22,27,-71,-105,-115,58,19,-101,0,11,34,87,-95,32,-128,21,38,83,-95,18,1,-75,-55,85,91,-94,114,117,-35,-110,-19,-93,78,-124,-126,-18,96,109,113,-83,26,-88,0,3,115,-49,-77,64,0,35,15,-1,-60,0,33,16,0,2,1,5,1,1,0,3,1,0,0,0,0,0,0,0,1,2,0,16,17,18,49,50,32,33,3,34,48,66,-1,-38,0,8,1,1,0,1,5,2,85,-91,-60,-72,-105,18,-30,92,75,-119,113,46,37,-31,23,-124,88,-81,44,110,112,48,-83,-65,-126,27,-47,121,-117,114,92,-44,45,-27,-108,76,-26,114,-64,-60,-36,94,98,-22,-127,97,111,0,-38,-85,-52,-53,-28,2,-48,-101,-6,74,47,52,81,24,-33,-38,110,47,49,69,-29,26,4,-104,9,-128,-104,10,-91,23,-103,-54,-45,57,-103,-118,73,-124,-38,-87,69,-27,69,-53,-97,-66,7,-64,-58,-26,-117,-88,-68,-113,-117,-31,4,115,-14,-125,127,-22,47,47,-27,116,-5,-94,-4,9,-44,94,95,117,27,-115,-44,2,-27,-52,77,-59,-28,-17,-59,-51,71,-22,34,110,47,62,-44,70,55,-94,110,47,45,-65,42,-79,-115,8,-76,77,-59,-27,-66,-113,1,109,11,81,54,-37,77,-59,-28,53,-91,-127,-104,25,-124,-72,16,-101,-41,-107,-119,-72,-68,-80,-79,-105,62,-77,-123,-82,16,81,13,49,19,17,49,19,17,49,19,17,49,19,17,49,20,102,-5,6,-65,-113,-28,-89,-1,-60,0,29,16,0,1,3,5,1,0,0,0,0,0,0,0,0,0,0,0,17,32,33,48,0,1,16,49,64,80,-1,-38,0,8,1,1,0,6,63,2,-19,9,-43,106,-103,39,38,-12,-55,40,24,55,-112,-54,61,33,9,-108,116,25,69,-108,82,111,11,97,-23,-72,117,-35,-1,-60,0,32,16,0,2,1,5,1,0,3,1,0,0,0,0,0,0,0,0,0,1,17,16,32,33,49,97,65,48,81,-111,113,-1,-38,0,8,1,1,0,1,63,33,83,82,-51,29,-114,-57,99,-79,-40,-20,118,59,16,-5,17,-75,51,80,-56,105,53,38,-24,-92,40,117,-3,48,18,120,42,-63,-23,100,22,-124,61,13,82,-112,-23,-43,123,22,28,61,-46,36,62,122,89,34,68,-109,112,-86,-10,38,84,22,-128,-10,-27,-49,-120,-86,-9,79,113,43,-97,20,-67,-110,-113,53,16,-44,-35,66,-27,-70,-66,-97,-83,83,37,20,-102,-16,82,85,112,-35,-120,-55,15,-85,18,-105,2,43,32,72,90,-71,78,-33,98,10,-106,80,-99,41,-88,123,73,10,61,75,35,13,44,-23,-88,105,-77,-110,83,117,49,6,8,89,-39,53,-67,-115,-47,50,-6,55,46,-49,123,-65,-36,74,-77,-44,44,61,-46,101,-24,-2,96,-108,-72,67,54,-77,-105,18,-51,-48,51,-31,106,-118,26,94,-51,-104,-2,-121,-102,-44,-108,54,-93,53,-77,95,-47,-39,-22,-88,118,27,111,110,-60,-31,-51,44,36,30,-44,76,67,26,79,103,51,-103,-52,-26,115,57,-100,-50,103,35,9,12,120,58,106,-8,-68,83,-1,-38,0,8,1,1,0,0,0,16,-128,1,120,-65,-29,93,-101,-66,-41,-10,-66,-79,103,-1,0,108,-1,0,-49,114,121,-13,-115,-97,-67,-5,-17,83,-33,90,126,-16,15,127,-33,-1,-60,0,33,16,1,0,2,1,5,1,1,1,1,1,0,0,0,0,0,0,1,0,17,49,16,33,65,97,113,81,32,48,-127,-95,-1,-38,0,8,1,1,0,1,63,16,-88,119,-16,-101,14,0,-99,25,-47,-99,25,-47,-99,25,-47,-99,25,-47,-125,-76,11,-128,80,-72,-56,2,-100,105,40,86,-117,-87,-40,68,-87,40,-4,-46,-16,-53,124,96,-42,34,38,66,32,-28,38,15,34,-93,76,-36,66,-74,-71,-118,-74,108,103,93,-20,40,-5,62,-55,-118,-88,42,10,-48,84,-66,32,98,103,1,-90,47,38,79,97,56,111,-10,43,79,-35,6,-62,-113,-111,62,48,-73,-99,88,68,39,-52,105,-117,-55,-109,-39,65,5,105,-56,-98,9,-14,95,31,-89,90,-29,76,94,76,-98,-24,53,-116,49,27,-83,-113,-37,108,113,90,98,-14,100,-10,39,89,-103,-43,-122,116,10,-105,114,118,51,-79,-119,11,108,118,83,79,40,105,-117,-56,-18,-67,-105,-59,-70,42,-74,-25,64,-128,54,-99,4,-34,-127,78,99,-75,-53,-126,45,-85,-93,28,7,76,94,75,-37,-64,-36,-39,-104,-4,24,-58,89,72,-7,-104,-84,-16,99,90,-122,-104,-68,-104,46,-21,22,-37,-4,111,44,-14,108,102,93,107,-48,-67,56,13,101,32,61,-4,-45,37,-64,-68,26,-105,-8,-87,-40,26,-53,29,-15,-8,20,59,-127,64,107,16,4,0,113,102,102,-13,77,-91,-114,-57,127,-127,-90,-55,-39,-118,-85,91,116,7,121,98,34,46,89,-101,-51,48,121,54,47,127,96,28,102,34,95,-63,-115,51,121,-84,-12,95,-90,120,68,23,-128,71,-88,-74,33,66,-103,-101,-51,54,-106,15,34,126,1,84,22,-61,22,85,113,27,-117,-94,-58,-90,9,114,38,111,52,-59,-28,-94,37,-90,13,72,126,78,-62,6,-29,8,-19,-64,-78,-28,-65,-51,1,90,51,3,-118,72,-27,25,-101,-51,99,-82,-37,56,-126,-104,106,118,102,80,127,21,63,17,14,96,-110,-110,-7,-107,11,51,-115,55,26,-109,16,90,2,127,16,0,0,0,91,-123,-35,80,17,5,-64,105,-1,0,23,-12,63,-1,-39,};	
	public static final byte [] uno6 = {-1,-40,-1,-32,0,16,74,70,73,70,0,1,1,2,0,37,0,37,0,0,-1,-37,0,67,0,27,18,20,23,20,17,27,23,22,23,30,28,27,32,40,66,43,40,37,37,40,81,58,61,48,66,96,85,101,100,95,85,93,91,106,120,-103,-127,106,113,-112,115,91,93,-123,-75,-122,-112,-98,-93,-85,-83,-85,103,-128,-68,-55,-70,-90,-57,-103,-88,-85,-92,-1,-62,0,11,8,0,-103,0,100,1,1,17,0,-1,-60,0,26,0,0,3,1,1,1,1,0,0,0,0,0,0,0,0,0,0,0,3,5,4,2,1,6,-1,-38,0,8,1,1,0,0,0,1,41,44,0,14,-71,-121,110,67,-78,-88,15,66,-50,58,82,40,97,-56,105,-41,-49,-68,-12,-86,81,41,103,-63,67,-116,64,-21,83,105,65,-42,-83,57,18,1,99,45,40,30,81,-56,-98,-8,2,-102,-87,64,123,115,-36,-11,17,2,-102,-87,64,-91,50,-54,-89,-46,-100,-94,-66,106,83,21,-97,-24,-72,-19,49,60,62,-122,101,44,-110,-113,-94,-99,63,-24,101,98,125,121,-12,-25,79,44,-15,58,-4,92,-76,-3,77,56,-119,27,105,-104,37,-69,67,85,74,7,-122,-3,39,-110,-24,-53,-90,-86,80,60,0,40,-28,77,53,82,-122,-80,10,11,-11,90,-43,75,12,-48,-10,-114,100,84,-52,-27,82,-106,-11,119,-42,5,-5,75,-82,85,74,42,-103,-57,32,80,-49,67,46,-99,-71,-64,0,-19,-112,-107,-12,-98,-128,0,76,-103,-1,-60,0,37,16,0,2,2,1,3,5,0,3,1,1,0,0,0,0,0,0,2,3,0,1,4,17,18,51,16,19,20,32,49,33,34,35,48,65,-1,-38,0,8,1,1,0,1,5,2,-57,77,105,59,-127,59,-127,59,-127,59,-127,59,-127,59,-127,59,-127,59,-127,40,-86,-31,-123,29,48,54,18,-8,-38,-37,97,86,55,-31,-127,107,47,111,-109,25,-101,-57,47,-112,56,-109,90,-71,-44,22,89,1,99,125,22,-110,57,-75,42,-98,77,79,36,110,18,68,-57,14,101,-14,7,16,-106,-42,48,65,-105,-108,122,-12,5,8,83,95,101,-24,-74,90,-20,42,-90,95,32,113,124,47,36,-93,27,108,-91,-82,-106,44,101,-78,-3,113,75,80,-53,-28,14,43,-5,16,-70,26,107,45,-123,54,21,122,-31,-52,-66,64,-30,-65,-87,94,-13,-56,110,-21,88,91,8,64,66,-73,-115,-57,38,-114,-70,-31,-52,-66,64,-30,-1,0,-91,-4,19,49,71,69,-27,-100,-38,99,43,42,-76,101,-47,31,76,65,-48,50,-7,3,-117,28,55,49,-25,-67,-112,43,64,-39,84,123,-58,-29,18,39,46,-76,-66,-117,-83,-85,-53,-28,14,46,44,126,-127,122,-122,89,94,-75,-8,-127,122,-122,85,104,-56,-127,-36,-63,45,89,-105,-56,-66,60,-78,-3,-70,98,-106,-85,-54,93,-108,5,-111,-40,-42,-125,-110,91,-103,19,-4,-107,-119,-7,-68,-66,69,-15,-70,-9,51,-94,-50,-42,64,-48,57,-83,84,118,71,69,6,-13,-55,57,-121,50,-7,3,-118,-2,-12,64,-82,-57,98,38,-60,77,-120,-123,95,-65,2,-90,28,-53,-28,14,43,-5,-20,-123,-48,-45,89,108,41,-121,50,-7,23,-58,-63,-38,126,-88,76,105,-9,79,-58,40,-60,-40,86,28,-53,-28,95,30,64,111,-82,-75,87,112,19,65,77,117,-99,-115,107,119,-37,76,-56,13,38,28,-53,-28,14,37,59,101,-38,65,-109,-59,57,88,-38,75,106,-41,12,-20,-18,85,-23,100,74,100,-55,-45,-75,-121,50,-7,23,-58,-64,-80,42,-69,-87,-36,57,101,119,-21,78,11,22,-78,-39,120,-95,98,57,124,-104,-19,-85,27,-86,41,-40,92,-15,-41,60,117,-49,29,115,-57,92,-15,-41,60,117,-49,29,112,84,3,46,-24,105,-89,-36,58,-5,95,63,-57,51,-89,-1,-60,0,41,16,0,2,0,4,4,5,5,1,1,0,0,0,0,0,0,0,0,1,2,16,17,49,18,33,113,-127,32,50,65,81,97,3,19,34,48,-111,66,98,-1,-38,0,8,1,1,0,6,63,2,-59,20,-71,-111,-52,-114,100,115,35,-103,28,-56,-26,71,50,50,101,25,65,104,120,62,81,80,-93,-6,40,-18,-115,-123,-96,-123,-118,42,11,54,-44,-5,35,-28,-22,-52,-96,41,20,25,24,-67,34,35,97,104,84,81,98,20,42,-46,-57,-22,126,20,-121,37,-61,-119,117,54,22,-110,-78,40,-46,61,-49,83,-13,-114,-99,-115,-123,-92,-3,-56,-49,18,-27,124,49,27,11,73,120,48,-85,20,50,41,84,85,95,-126,35,97,105,42,127,78,85,-18,40,17,90,51,52,-22,54,-89,94,-26,-62,-48,-85,-24,57,36,56,-39,74,-93,-55,73,-92,108,45,15,46,105,-118,30,-110,78,104,-119,118,54,33,-48,80,-50,-99,-116,72,-94,66,70,-110,113,-66,-92,70,-60,58,13,-50,-90,78,84,-125,-10,84,48,43,34,35,97,105,-63,-13,-71,-45,-12,-70,-3,58,126,-115,66,127,-89,40,-115,-123,-89,-47,-18,70,120,-108,70,-62,-48,107,-117,28,118,48,-85,23,69,91,68,70,-62,-48,-9,33,-32,-56,-59,-22,51,-64,-112,-95,-117,54,40,-31,-77,34,54,22,-123,29,-118,-63,17,116,124,-30,41,-23,-84,-52,-27,81,56,-86,-103,9,17,-80,-76,51,50,57,-103,-101,-31,88,-31,-51,74,-81,-87,-79,-123,-36,-51,28,-91,-117,22,44,88,-79,99,40,74,-78,-65,100,50,-1,-60,0,36,16,1,0,2,1,3,4,3,1,1,1,0,0,0,0,0,0,1,0,17,49,16,33,65,32,81,97,-15,113,-127,-16,-95,-111,48,-1,-38,0,8,1,1,0,1,63,33,-84,43,-68,19,98,123,41,-20,-89,-78,-98,-54,123,41,-20,-89,-78,-98,-54,97,-42,87,-40,-114,-12,-22,-96,-91,-74,38,-63,54,-15,63,-52,79,77,61,-103,79,104,42,-79,-88,-97,104,76,90,40,-124,-9,-105,-28,-72,38,-13,70,47,93,-33,-20,103,-14,-108,-81,96,-82,33,24,91,15,-83,28,90,41,-120,-16,-54,93,32,-67,-56,-25,67,43,93,-70,91,-73,19,28,-111,-101,116,25,-117,69,94,-27,93,51,-45,-64,0,124,78,108,113,23,23,28,29,94,127,24,-76,86,77,24,-115,6,46,32,111,-64,-128,-72,-120,-83,3,-29,-86,-59,-94,-78,76,-42,27,-77,112,-10,-1,0,101,103,-10,-54,-40,62,96,-86,51,-38,36,10,-108,70,-100,-12,88,-76,85,45,76,-36,84,-99,16,-18,-95,60,-115,-39,-60,-43,-52,-85,-13,16,-61,-95,-42,-58,-71,76,90,40,-109,6,-7,96,-32,-40,-122,103,-118,-119,104,119,-30,-8,-126,-24,-34,38,-1,0,85,-36,70,101,-109,93,-78,-83,-90,45,20,-18,-90,-38,38,103,-102,-119,-29,69,-60,-95,50,68,107,41,11,103,-109,68,-16,-9,-107,-10,40,76,90,34,-64,59,26,-121,117,20,-105,-87,-75,75,8,-98,8,32,-83,120,104,12,-89,8,-20,-68,-52,90,50,-106,-17,-83,103,-10,78,-24,118,98,59,80,33,85,-37,-13,-95,-56,99,-104,42,105,-74,45,21,-109,84,-19,55,114,-49,62,-128,60,-16,12,-127,123,64,47,102,-86,-37,-99,44,90,43,39,94,0,6,46,49,95,-127,-83,-117,71,120,61,-22,10,-64,48,49,-59,115,-94,123,-7,103,15,-115,44,90,58,-125,119,-65,66,52,45,-108,-19,-15,42,70,-35,-79,12,-54,-60,32,91,-104,35,65,-40,-76,85,-42,-13,127,-56,-30,99,-58,-128,91,-52,38,-38,94,-23,110,127,90,49,12,-110,-19,-10,49,-46,54,-33,99,75,20,110,-103,-39,-121,12,70,-46,51,-39,76,-104,-4,-12,-118,93,-40,-122,-10,-96,-63,16,-3,89,-118,13,53,15,-20,50,-119,60,-49,-39,-97,-101,63,54,126,108,-4,-39,-7,-77,-13,103,-26,-59,-84,35,-108,-96,-105,-100,120,-104,38,15,-6,31,-1,-38,0,8,1,1,0,0,0,16,-64,22,-128,4,8,42,4,-127,7,-128,-30,13,66,93,-94,-23,-126,40,-108,-64,58,0,35,0,-112,-84,38,-123,0,47,-10,0,15,-1,-60,0,40,16,1,0,2,1,3,3,4,3,0,3,1,0,0,0,0,0,1,0,17,33,49,81,-95,16,65,-79,97,113,-63,-15,32,-111,-16,-127,-47,-31,48,-1,-38,0,8,1,1,0,1,63,16,65,101,-85,48,122,-59,12,-96,79,-83,79,-83,79,-83,79,-83,79,-83,79,-83,79,-83,79,-83,75,-68,94,-76,-60,-127,113,-121,-71,-19,51,-9,89,29,-56,-87,-10,30,34,16,26,69,-81,19,70,-81,-26,41,75,53,14,-25,-29,126,-97,-94,36,90,-113,-15,14,41,25,17,-118,-83,112,-85,-68,-31,-89,27,-30,22,-74,89,-3,65,-73,-91,3,-113,121,101,6,-55,-35,117,32,36,97,-41,66,106,-49,31,-18,118,-91,-58,85,-119,96,33,109,76,103,117,-68,-89,30,-34,-16,34,29,74,-100,52,-29,124,77,116,-65,105,19,-75,66,-51,-49,-120,-95,1,-76,119,96,43,70,-77,88,-78,-45,-26,88,89,93,59,-79,85,-75,-66,-89,-34,-34,-82,-119,18,72,18,-127,-52,-31,-89,27,-30,26,91,34,-99,30,-106,92,32,-40,-116,-51,4,12,-74,-65,113,-5,-47,-10,67,-14,-79,78,94,25,-61,78,55,-60,-28,61,4,32,-81,-26,-114,85,70,-74,-119,-96,23,-38,86,112,-18,-62,35,73,95,-121,-61,56,105,-58,-8,-100,-122,49,104,-50,-4,69,65,105,-75,-35,12,97,53,-40,33,66,-88,-54,-43,-9,-123,-106,-76,8,-54,-50,-90,19,-65,-92,118,72,26,71,-73,95,-122,112,-45,-115,-15,22,-55,85,0,-17,61,-41,30,-101,-57,44,53,82,-59,-70,-52,14,65,-40,119,-106,12,-70,-16,74,-123,3,3,65,-120,-108,-105,116,-21,-42,-60,-93,-127,-36,39,13,56,-33,16,94,110,-53,-42,-15,18,-46,-33,6,-60,22,13,-30,-95,86,35,81,17,26,101,-96,-62,-117,93,2,48,71,75,-57,114,9,-12,-108,-12,5,64,-43,-102,-14,2,-49,94,-13,-122,-100,111,-120,-79,5,57,61,127,-28,114,-59,65,-38,42,21,98,-75,2,22,-65,97,-118,90,37,-119,52,-64,43,80,-86,41,53,-11,122,3,23,74,-51,105,82,-24,97,-64,-30,-25,13,56,-49,17,-60,43,-76,55,-22,106,-27,-119,87,-104,32,6,-80,59,111,0,-22,25,83,4,18,92,17,-103,-40,-24,-49,126,-116,101,14,15,125,-94,-90,85,10,-77,-122,-104,-5,15,16,-21,-107,-127,78,-75,-44,-58,83,77,-62,80,-126,51,97,-117,59,-36,-40,66,59,-21,1,-37,-38,42,-74,-21,3,44,50,-10,33,121,90,-107,-91,-49,-122,112,-45,-115,-15,57,15,87,-92,-32,48,98,0,-40,112,-87,74,116,-23,-124,100,-124,-51,-19,17,-121,99,93,62,-93,-74,84,-38,-67,-25,-61,56,105,-58,-8,-100,-121,-13,-58,-16,88,28,-60,9,-93,91,71,79,-122,112,-48,89,-18,60,70,64,123,23,-73,-28,-96,102,87,113,-22,-6,74,96,-44,46,11,-35,-24,96,68,22,-87,92,-8,103,13,21,62,-61,-60,33,-44,-84,77,127,3,47,109,0,-72,112,74,-55,124,127,-99,-30,69,79,1,-17,-17,20,-86,16,48,104,-123,-76,-72,-104,-70,-43,-40,103,-61,56,105,-58,-8,-116,112,-53,95,40,-16,-57,47,111,-4,-97,122,-63,-86,103,32,-4,-64,-24,-94,-74,-2,-5,-58,-81,115,-114,-61,-37,-90,-95,-96,-112,2,81,72,37,40,-52,41,-124,43,105,-16,-50,26,26,123,-125,-120,83,45,-76,48,-54,-61,93,-58,-89,-34,-93,54,-2,-103,95,-32,52,-36,-69,30,-62,53,-118,-114,-98,25,-104,-64,-62,-87,-95,56,105,82,39,69,-10,79,70,4,46,44,-1,0,-78,127,21,63,-118,-97,-59,79,-30,-89,-15,83,-8,-87,-4,84,-54,-125,91,115,80,-15,26,-29,8,33,-64,118,-115,40,107,36,-32,31,-7,54,-115,-17,-45,-1,-39,};
	public static final byte [] uno5 = {-1,-40,-1,-32,0,16,74,70,73,70,0,1,1,2,0,37,0,37,0,0,-1,-37,0,67,0,40,28,30,35,30,25,40,35,33,35,45,43,40,48,60,100,65,60,55,55,60,123,88,93,73,100,-111,-128,-103,-106,-113,-128,-116,-118,-96,-76,-26,-61,-96,-86,-38,-83,-118,-116,-56,-1,-53,-38,-18,-11,-1,-1,-1,-101,-63,-1,-1,-1,-6,-1,-26,-3,-1,-8,-1,-62,0,11,8,0,-102,0,100,1,1,17,0,-1,-60,0,25,0,0,3,1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,3,4,2,5,1,-1,-38,0,8,1,1,0,0,0,1,44,-64,0,30,-100,-5,-31,-46,-46,0,123,125,43,-25,93,44,-29,104,-16,51,122,-7,-52,-93,-97,102,101,13,-12,-104,-66,93,9,-87,9,0,-66,-91,-14,-4,-83,73,0,42,-67,124,-74,-78,91,-76,74,-110,-85,-41,-53,-74,29,-11,97,18,-78,-22,-41,30,19,71,65,126,64,-77,-88,-43,-90,2,-122,-51,126,121,-37,-24,-73,18,72,85,-20,-105,55,-103,97,110,57,-85,31,-47,91,57,-53,-89,55,-29,-107,-32,105,-104,-59,-47,-47,122,-7,126,0,21,97,21,95,-114,102,0,40,-44,-2,83,126,37,-116,10,-4,77,-118,-14,-11,-53,-22,88,-55,-107,-85,61,85,-8,-27,-116,-58,3,-38,-62,-44,49,32,0,123,68,-111,-11,-64,0,9,-71,-1,0,-1,-60,0,35,16,0,2,2,1,4,2,3,1,1,0,0,0,0,0,0,0,1,2,0,3,50,16,17,18,19,32,33,34,49,65,48,51,-1,-38,0,8,1,1,0,1,5,2,-86,-67,57,44,-28,-77,-110,-50,75,57,44,-28,-77,-110,-50,75,57,3,10,-122,-113,89,5,112,102,44,-3,45,26,-78,-93,-50,-89,-27,-94,-32,-71,-72,-36,-40,-84,53,90,-53,78,40,-109,-75,68,-19,83,10,43,10,115,-117,-126,-115,-35,-47,-73,-77,-43,81,43,0,61,-66,10,-36,72,-40,-104,-72,126,-9,52,102,45,17,56,-117,31,-111,-15,-96,-6,-117,-121,-20,-87,54,-106,63,35,-27,70,81,112,-3,-83,121,53,-83,-96,-92,109,-46,-77,-91,101,-118,20,-23,70,81,112,-3,-1,0,58,-30,-28,125,14,-26,-99,-51,25,-71,29,40,30,-94,-31,82,-18,-42,-73,38,-108,-113,-103,27,-127,90,-120,-43,41,-116,-91,78,-107,13,-110,46,24,87,-91,78,18,53,-34,-71,-76,-87,-7,11,-121,-58,40,-35,-73,-7,-60,-58,-13,-83,117,-122,22,85,-59,101,3,-43,-72,74,-121,17,81,-34,-56,-104,-39,-19,-12,-87,-8,-97,-71,-42,-77,-48,22,-65,34,-93,-111,-76,-20,40,-54,46,7,-17,80,-60,78,-42,-123,-119,-47,7,90,19,-71,-93,40,-72,126,-7,84,-110,-57,-28,101,25,69,-63,-67,55,-115,117,-14,-106,60,-30,76,35,105,70,81,48,-71,124,82,-88,-10,68,27,-75,-113,-62,59,-122,74,50,-117,-126,89,26,-83,-25,83,65,73,-97,10,-29,57,109,1,-40,-77,35,-121,85,21,81,-108,92,24,108,-64,-111,59,90,22,39,-64,125,-10,87,30,-64,86,-123,-46,-90,-35,74,-122,-99,43,58,86,116,-84,-23,89,-46,-77,-91,103,74,-50,-107,-99,43,62,-93,-37,-77,15,-65,-27,126,51,-1,-60,0,33,16,0,1,3,5,0,3,1,1,0,0,0,0,0,0,0,0,0,1,17,49,2,16,18,32,33,48,81,97,50,113,-1,-38,0,8,1,1,0,6,63,2,117,-76,-110,73,36,-110,73,36,-99,56,32,-34,38,93,63,76,117,93,52,-22,-100,-92,-19,35,-47,-81,-77,-74,-54,-95,-87,-41,36,-41,-90,85,31,54,109,114,-88,-7,-28,-59,53,100,-33,-19,-109,117,91,-65,-81,47,-10,-3,56,77,-98,-19,100,27,71,75,61,-14,81,86,-56,46,-79,118,49,-70,107,-59,36,-22,-37,37,31,-57,-110,-63,-13,68,23,103,88,49,72,32,-19,-48,-55,53,122,-122,-90,-52,-121,-35,26,-88,30,-110,14,-98,-42,-18,116,-27,-46,-46,73,58,-2,70,65,-17,-33,27,39,-101,-1,-60,0,34,16,0,2,2,1,5,1,1,1,1,1,0,0,0,0,0,0,0,1,17,49,16,33,65,81,97,113,32,-95,-127,-111,48,-1,-38,0,8,1,1,0,1,63,33,84,127,-102,40,-22,-99,83,-86,117,78,-87,-43,58,-89,84,84,-112,81,8,64,-47,-76,126,66,52,122,81,-36,-119,-109,-113,-88,19,106,-121,-90,-30,-59,62,20,-6,61,63,-28,37,-56,85,-24,-123,-66,-103,32,-92,20,-24,-41,-127,97,-109,-29,20,-8,104,-45,2,59,-5,20,45,-113,-64,35,94,-62,27,-105,-82,93,34,21,13,-27,-118,124,38,36,-124,-123,-77,27,-123,-126,-33,-87,35,-16,-59,62,14,-40,-127,-75,-80,-33,-71,111,-104,-89,-63,-40,-15,-111,-26,44,58,-83,-73,39,115,59,-104,-73,-110,-33,49,79,-124,76,23,35,-81,118,22,81,-40,-46,-66,14,-92,117,33,-13,63,-104,83,-31,46,-44,60,21,-119,-91,-63,48,-71,22,-44,-6,108,49,-31,26,121,-127,-59,62,14,31,-50,82,28,-124,-45,-87,-83,50,44,-19,19,-11,98,57,27,61,-106,63,17,-86,-109,108,-69,-74,-11,17,-88,97,-55,-71,15,-81,19,57,-116,-4,67,100,89,-45,52,78,81,-85,33,-66,18,40,-23,16,-88,71,-87,111,-104,-4,-61,-53,63,-117,19,-31,121,49,107,28,-26,45,-13,20,-8,59,125,-95,-16,-115,33,83,22,-7,-113,-56,44,46,-2,-32,91,66,84,-104,101,16,91,-26,63,7,-42,117,104,-99,84,-120,-12,-58,-70,73,-87,-51,-66,98,-97,4,41,34,117,63,-52,39,-81,4,-44,27,-12,46,48,-27,-90,-62,52,-31,-110,4,-2,-106,-7,-113,-56,72,34,-111,-106,21,-77,124,65,36,-42,3,122,-104,-55,111,88,74,-106,-24,-82,29,-52,-18,103,115,59,-103,-36,-50,-26,119,51,-71,-119,15,118,40,78,16,-3,37,-114,-46,67,21,47,-7,51,90,121,-57,-1,-38,0,8,1,1,0,0,0,16,-1,0,-28,6,108,-49,-10,93,64,-52,6,114,62,3,-24,13,33,-16,75,-116,84,104,-64,6,7,-80,-107,-46,10,-94,-16,0,127,-1,0,-1,-60,0,38,16,1,0,2,1,4,1,4,2,3,1,0,0,0,0,0,0,1,0,17,33,16,49,65,81,32,97,113,-95,-15,48,-16,-127,-111,-47,-79,-1,-38,0,8,1,1,0,1,63,16,81,43,-67,-56,-96,-50,2,125,-52,-5,-103,-9,51,-18,103,-36,-49,-71,-97,115,62,-26,49,76,122,25,84,76,90,-123,-111,8,-23,88,108,-79,-76,47,26,73,-25,66,-42,60,65,118,22,40,-36,79,120,-115,-92,123,35,6,-78,-61,-39,-31,-2,-96,101,40,-19,11,-85,102,29,114,67,-44,96,-40,-113,-120,-31,-33,120,20,13,112,-59,100,-90,97,-114,82,100,107,-20,102,-11,105,-22,11,47,-82,29,45,108,15,-26,5,-76,65,107,65,-111,68,-97,117,-27,-120,-118,85,-27,-44,123,53,121,59,-107,-32,52,58,-6,-99,41,28,48,-32,-108,114,-64,106,-66,-126,98,-47,89,7,-119,78,48,54,60,-81,103,42,-11,-1,0,-104,-24,8,0,75,15,-3,-108,-29,3,99,-15,103,-4,-58,56,95,114,-61,81,-21,126,-71,-101,-63,81,69,-90,-77,55,32,-39,106,-7,103,-87,66,-41,8,-21,24,-102,-86,-82,-20,7,11,17,15,-120,17,-69,20,-23,37,-116,16,-84,107,69,-51,-38,-41,-7,16,92,-76,-89,-128,-48,47,112,-78,100,-38,-91,74,-91,87,44,1,-117,98,-76,-31,-39,-17,84,10,82,-25,95,-77,11,-2,-109,125,48,-110,-16,-88,-74,48,-7,120,-99,-75,119,-68,70,-9,-48,59,-49,70,60,-69,115,12,109,-116,-70,124,52,37,-18,-55,-85,34,3,-79,44,-54,71,55,-94,-104,-93,100,32,-44,23,109,14,-102,21,-123,-114,-105,35,-89,-61,69,91,-35,106,11,-23,-97,72,59,-15,8,-75,-26,-27,-119,66,-127,71,88,-11,-113,-54,24,29,127,46,-72,-87,14,37,30,-35,95,6,-84,31,-52,-3,-60,-36,68,-22,111,4,0,109,12,86,114,-66,25,-1,0,49,-13,-75,26,-56,95,49,111,122,-113,12,54,44,99,10,12,15,37,91,103,-26,99,24,48,-89,48,-117,97,-40,69,-23,23,-82,-71,-124,86,38,97,-34,-68,1,90,11,101,2,52,-105,76,42,-6,112,-67,-62,-37,-53,-104,3,118,-22,14,-60,59,30,25,-18,101,-91,5,-30,9,69,28,-27,63,115,2,-79,49,45,3,-5,35,-88,-12,70,-101,-32,40,-78,-95,20,15,98,-71,107,-126,-53,-71,118,-17,12,-8,120,103,-18,37,-107,-95,-30,-26,-6,-92,5,-121,36,40,-1,0,16,120,-81,104,80,17,20,122,-24,-59,67,21,66,26,85,-25,-97,-61,51,51,51,50,113,90,56,96,0,80,68,-128,6,-21,21,-104,111,114,103,-19,126,32,54,75,-31,55,-97,-1,-39,};
	public final byte [] uno4 = {-1,-40,-1,-32,0,16,74,70,73,70,0,1,1,2,0,37,0,37,0,0,-1,-37,0,67,0,27,18,20,23,20,17,27,23,22,23,30,28,27,32,40,66,43,40,37,37,40,81,58,61,48,66,96,85,101,100,95,85,93,91,106,120,-103,-127,106,113,-112,115,91,93,-123,-75,-122,-112,-98,-93,-85,-83,-85,103,-128,-68,-55,-70,-90,-57,-103,-88,-85,-92,-1,-62,0,11,8,0,-102,0,100,1,1,17,0,-1,-60,0,26,0,0,2,3,1,1,0,0,0,0,0,0,0,0,0,0,0,0,5,2,3,4,1,6,-1,-38,0,8,1,1,0,0,0,1,-98,-6,-64,0,-24,-111,-14,26,-27,80,1,-41,27,43,66,-49,62,19,70,-56,119,-111,111,82,125,61,88,-58,-68,65,107,-85,-22,-13,-46,-98,-52,-76,0,55,-33,87,-98,24,-26,-50,0,111,111,87,-98,-45,110,18,-26,42,3,123,122,-68,-5,53,101,-49,39,-26,-8,54,97,82,-70,104,-71,-34,77,-2,111,-125,-5,-22,-60,-84,123,-114,-73,30,122,-69,-101,106,-30,-116,35,-37,59,60,105,-103,69,-79,-25,-23,3,75,-49,55,61,-80,113,15,56,1,-39,-42,-47,110,-57,21,121,-32,0,97,70,109,-50,57,-25,96,1,-77,-71,35,-71,-57,20,-32,1,-115,121,-100,97,28,84,-73,-76,91,118,42,6,-12,-63,-60,124,-15,117,48,1,-118,-10,76,-13,-53,48,0,19,-42,-85,15,-93,-24,0,2,-43,127,-1,-60,0,36,16,0,2,2,1,3,5,1,1,1,1,0,0,0,0,0,0,2,3,0,1,4,17,18,51,16,19,20,32,49,33,34,35,48,-1,-38,0,8,1,1,0,1,5,2,66,107,111,-55,-36,9,-36,9,-36,9,-36,9,-36,9,-36,9,-36,9,-36,9,70,55,8,4,-23,-85,-40,127,41,-84,-77,32,11,59,98,-19,119,-19,-14,99,51,125,70,113,-128,-39,-107,88,-90,101,-14,116,90,72,-26,-59,46,-68,-127,-87,-28,1,66,72,29,98,-2,54,51,-115,7,75,33,36,-39,-27,88,116,90,104,69,-71,22,94,-117,59,11,26,-85,40,-50,57,87,-91,-80,-19,-106,-75,-46,-59,-115,38,123,98,22,-95,25,-57,-47,11,-95,23,51,-72,94,-40,-105,-3,-58,113,-60,47,121,100,55,91,-24,-75,-37,47,-59,15,76,74,-2,-29,56,-2,-62,-66,-62,122,45,118,-53,0,-96,-85,-7,127,122,98,87,-13,25,-57,-118,26,-109,-113,121,-59,-82,-39,98,34,-79,-89,111,117,-4,-65,-67,19,91,87,25,-57,127,-27,-113,-47,3,-75,111,35,56,-128,42,109,-4,32,42,-24,-95,-34,-51,-38,-66,95,-20,-53,47,-21,-94,-52,118,119,2,81,-115,-12,121,-115,-86,99,-42,-64,-58,-67,91,-47,-73,-85,61,49,-7,-81,-27,-3,88,-17,44,-109,-46,-79,57,33,94,-125,-21,87,-92,-18,31,69,-24,-123,21,-18,-68,78,72,-50,63,124,117,71,-77,-72,83,19,-109,-95,-42,-123,-22,-124,-17,-113,110,-77,109,-43,76,78,73,119,-91,100,-121,-17,-94,-111,-92,107,-75,-117,29,-26,106,-36,-74,99,-20,12,78,72,-50,52,-69,-16,-79,-89,-114,-56,56,-59,53,82,99,26,71,-44,-58,-51,45,-79,90,-15,57,37,-42,-76,67,-76,-88,-118,-89,125,-110,-52,-81,-41,-56,-47,127,102,32,116,75,55,-127,-84,78,120,-95,60,80,-98,40,79,20,39,-118,19,-59,9,-30,-124,-15,66,14,48,85,-4,-115,121,111,27,-70,-70,-7,-1,0,28,-69,-66,-97,-1,-60,0,39,16,0,1,2,4,5,4,3,1,0,0,0,0,0,0,0,0,1,0,16,2,17,33,49,18,32,34,50,113,3,81,97,-127,19,48,65,-111,-1,-38,0,8,1,1,0,6,63,2,-59,16,-85,110,11,112,91,-126,-36,22,-32,-73,5,-72,45,-63,80,-123,80,-92,-34,20,-126,-111,-6,36,110,26,46,20,-126,16,10,-60,110,-67,100,-44,102,86,-104,22,-88,20,-6,71,-46,45,23,10,101,13,38,106,-38,-101,31,81,74,26,12,-108,95,32,-20,-47,112,-45,83,43,-28,-22,127,23,-116,-46,-20,-47,112,-1,0,36,107,-58,114,26,46,27,-64,88,69,-125,-47,92,-28,37,-94,-31,-80,-2,-105,-94,-112,-54,75,69,-62,-60,127,20,-38,-116,0,-74,80,26,46,23,-110,-31,72,66,100,-124,-63,105,-112,88,5,-121,-80,113,15,103,26,-126,-36,21,8,98,1,12,122,-123,18,-15,101,15,36,58,99,-15,122,98,115,81,110,45,-118,43,-107,50,-67,52,92,125,24,-30,-78,-16,27,-45,-111,-102,113,109,88,97,-38,-89,38,-12,-45,88,-57,-18,92,93,69,-122,10,66,-92,-124,51,-78,-59,-118,107,-45,69,-62,-61,29,-108,-32,51,86,90,-116,-107,43,18,-85,-64,2,-8,-59,74,-12,-46,68,42,21,-71,84,-100,-96,67,118,49,55,-107,80,-82,85,-54,-71,87,42,-27,92,-85,-107,114,-69,-74,-125,69,79,-84,55,-1,-60,0,37,16,1,0,2,1,4,3,0,2,3,1,1,0,0,0,0,0,1,0,17,49,16,33,65,81,32,113,-15,48,-127,97,-111,-16,-95,-31,-1,-38,0,8,1,1,0,1,63,33,56,-80,-69,-118,11,90,39,-46,-97,74,125,41,-12,-89,-46,-97,74,125,41,-12,-94,52,-89,-36,-88,93,25,-123,-89,17,75,28,17,118,-17,66,81,125,-27,-124,47,-8,-15,-85,-107,80,85,99,76,85,123,-123,29,105,2,15,120,-106,-4,110,-104,-75,96,-54,59,102,58,57,125,-90,-91,-114,57,-64,-28,20,9,77,107,10,109,-86,-83,-94,-79,101,-53,46,82,-74,108,-23,-114,53,-67,120,-125,-33,-42,-36,-112,122,-26,-98,16,-58,50,65,96,94,54,-100,-40,-30,24,-35,-82,7,-107,-115,-14,-15,-116,0,-84,92,91,48,48,121,-73,66,-68,33,127,-68,-101,-41,-35,-83,87,14,88,-127,7,58,-73,66,-75,-128,80,25,101,72,-83,106,-82,28,-80,41,-52,-109,38,-75,-9,29,96,31,4,-35,-16,54,52,-86,-31,-53,42,38,-63,-106,63,-2,-17,50,76,-102,-38,-39,-83,99,15,84,107,115,-69,18,-83,-11,-52,105,17,-22,100,-106,1,30,-76,-21,14,97,-80,99,64,1,28,50,-62,112,-44,102,-39,93,-49,-91,18,-91,62,-12,97,39,-34,-125,-6,108,103,-78,-102,-78,-98,-4,-82,73,-110,40,-97,-36,12,7,41,-101,69,-111,-63,29,-33,20,86,-87,-97,74,102,10,20,-107,82,-42,102,-4,72,20,-58,49,23,-6,13,51,104,119,42,127,6,62,72,-79,15,-5,9,95,-77,-88,-38,16,60,-23,-101,64,34,-63,54,-37,-57,-117,-115,6,106,63,-88,123,-108,-105,-85,-26,98,-1,0,105,97,-127,51,106,-127,50,-8,-78,-19,-104,-97,-26,-59,110,19,-66,5,83,38,-37,-93,92,-76,-19,46,53,-103,-103,-76,19,-82,99,35,-60,-50,-92,-1,0,98,100,87,-17,-58,-94,-102,-107,113,85,107,108,66,-33,70,-128,77,-20,-52,63,-101,-65,-62,0,0,0,1,102,-33,-38,20,58,33,-111,-80,-128,84,-109,7,-30,32,69,7,58,127,-1,-38,0,8,1,1,0,0,0,16,0,35,-122,24,40,46,65,-63,8,8,66,68,0,96,2,100,-112,-56,4,-116,28,0,2,7,8,-48,56,-92,64,-97,-15,0,47,-1,-60,0,40,16,1,0,2,1,2,6,2,2,2,3,0,0,0,0,0,0,1,0,17,33,16,49,65,81,97,113,-95,-15,32,-79,-63,-47,48,-111,-127,-31,-16,-1,-38,0,8,1,1,0,1,63,16,-35,61,3,96,71,-94,3,117,-38,122,-44,-11,-87,-21,83,-42,-89,-83,79,90,-98,-75,61,106,116,-93,68,84,49,-31,-52,-126,-46,114,-85,-124,96,-128,22,-12,-114,41,23,-95,80,-99,-123,-69,-64,-123,-100,69,-4,66,-40,47,98,43,112,-99,-56,97,65,-78,51,-102,65,59,-92,83,112,-9,39,-100,-105,-32,91,-68,-114,112,-39,0,57,19,-61,107,98,-43,-12,70,22,-20,23,87,-98,-43,1,-58,92,112,44,12,2,-101,56,99,50,88,-51,-73,-3,71,28,12,71,-122,116,-13,-112,98,74,74,-31,-51,39,31,-18,2,-40,-57,36,46,2,-76,111,9,53,48,63,-49,-22,37,106,7,12,44,85,109,109,-44,91,4,-99,66,28,103,50,-72,-23,-25,52,53,-123,108,-72,52,-128,-95,27,8,25,109,-3,-57,56,-106,-57,99,-27,98,89,-64,114,29,60,-26,-81,14,-103,31,-76,-87,-121,3,-14,124,-56,103,43,-90,-98,115,64,75,67,11,120,-12,-116,-75,-80,117,58,-101,42,61,-128,-120,92,-95,-52,-122,-103,-55,-43,-122,48,-86,-23,-25,33,-107,105,64,75,-118,-115,-76,-57,118,42,-74,-17,-95,-78,-93,-40,9,83,0,-35,-30,-77,-64,103,-112,-22,89,123,-93,-6,-45,-50,74,8,-86,-13,-50,40,-49,-44,104,108,-88,-10,2,97,89,-83,56,-11,98,28,-123,124,39,-128,-49,33,-48,-53,31,-98,6,-54,-33,76,-5,-56,65,6,26,111,-101,-2,-75,-34,-94,110,-17,31,-100,-68,-48,109,85,-38,-31,-76,-53,-80,-54,57,46,-18,26,48,69,-38,-5,33,-40,14,95,118,-76,18,-20,41,32,-72,-58,110,-18,-89,-28,8,-114,-55,-21,80,91,86,-63,-108,112,70,-30,107,6,91,-23,-115,-85,19,-78,57,54,-123,127,-50,-84,-59,-73,63,-81,-113,-36,-6,-98,3,60,-122,15,-85,110,92,-114,115,23,96,126,-92,-14,-38,21,-94,-111,106,59,94,111,-60,59,11,100,115,22,63,42,101,115,88,-89,-127,-125,-113,66,36,68,90,-77,-53,105,-25,62,-65,-128,3,25,97,121,113,101,36,-85,-112,-100,122,-23,-27,-76,22,115,21,50,-3,-40,95,-56,65,48,9,24,26,118,11,-6,-122,54,116,-116,58,121,109,17,26,11,88,66,60,93,108,114,-8,2,-76,22,-64,5,4,-93,-4,-57,28,65,-80,-81,72,77,-17,-78,-126,-127,52,-39,-67,64,24,-32,-64,79,45,-89,-100,-101,-34,10,66,-21,-92,92,-31,44,23,51,-2,34,26,8,-27,-69,43,-20,66,-9,111,-68,86,-85,59,98,13,5,27,26,-105,79,108,-101,-40,-83,-30,52,83,93,-19,60,-74,-128,37,-127,24,92,-46,-86,113,27,-34,-103,-118,-66,-120,85,9,119,76,111,-65,-63,61,65,-77,99,-84,114,-28,109,94,48,-63,-96,-99,-51,29,3,104,-15,-71,64,7,-128,110,79,122,79,122,79,122,79,122,79,122,79,122,79,122,79,122,64,12,-85,-126,-60,7,0,3,6,-63,12,-87,-118,-62,-19,-118,-123,-67,-58,-90,93,-125,-8,-81,122,20,29,-12,-1,-39,};
	public final byte [] uno3 = {-1,-40,-1,-32,0,16,74,70,73,70,0,1,1,2,0,37,0,37,0,0,-1,-37,0,67,0,40,28,30,35,30,25,40,35,33,35,45,43,40,48,60,100,65,60,55,55,60,123,88,93,73,100,-111,-128,-103,-106,-113,-128,-116,-118,-96,-76,-26,-61,-96,-86,-38,-83,-118,-116,-56,-1,-53,-38,-18,-11,-1,-1,-1,-101,-63,-1,-1,-1,-6,-1,-26,-3,-1,-8,-1,-62,0,11,8,0,-102,0,100,1,1,17,0,-1,-60,0,25,0,0,3,1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,3,4,2,5,1,-1,-38,0,8,1,1,0,0,0,1,43,-56,0,30,-100,-66,-97,63,106,88,1,-17,65,21,-13,95,-72,-122,-47,-97,76,-107,-13,-102,-24,43,-52,-64,-50,-116,-107,-14,-24,77,83,44,3,-94,-118,-7,126,86,-107,0,22,-30,-66,91,89,45,-49,76,62,22,-30,-66,93,-80,-43,95,63,-95,36,-89,65,53,-57,-124,-17,-42,-37,2,14,-84,-107,-94,16,45,-86,41,89,-46,-114,-55,36,45,34,-24,111,-103,102,-105,103,53,99,-70,75,103,57,116,107,22,114,-68,13,51,24,-70,43,49,95,47,-64,2,-91,-90,-36,89,-52,-64,5,26,70,108,-59,-110,72,5,121,93,107,-9,21,-53,-22,89,-71,-44,-53,34,-85,22,114,-58,99,1,-86,-46,-11,-48,-60,-128,1,-21,-7,-85,-21,-128,0,18,67,-1,-60,0,36,16,0,2,2,1,3,5,1,1,1,1,0,0,0,0,0,0,1,2,0,3,50,17,18,19,4,16,32,33,49,34,51,65,48,-1,-38,0,8,1,1,0,1,5,2,-86,-71,-14,110,89,-71,102,-27,-101,-106,110,89,-71,102,-27,-101,-106,110,6,50,-122,12,-69,74,-32,-20,88,-83,58,-57,93,-89,-50,-90,-36,47,-55,112,7,70,-43,-84,107,-49,-82,-53,89,105,-75,18,114,-88,-100,-118,99,86,24,83,-99,-7,46,10,53,118,70,86,-77,-7,-59,-81,72,-10,-21,-32,-115,-76,-128,53,-65,37,-61,-3,-26,104,-52,90,34,-118,-61,-71,99,-29,73,-4,-33,-110,-32,126,-54,-45,72,-19,-72,-7,116,-14,-4,-105,3,-10,-75,-36,109,126,-53,76,-40,-79,-87,30,29,60,-65,37,-61,-3,-2,117,-54,70,-91,-37,104,-28,109,81,-73,45,-29,-33,106,49,-65,37,-62,-91,-43,-20,109,-51,21,-118,-62,-27,-96,-92,-104,-93,98,-38,-37,-101,-75,99,68,-65,37,-61,10,-4,22,-32,0,33,-123,-87,-89,100,26,-72,63,-85,-14,76,111,62,-5,82,1,23,47,-87,70,54,-31,43,-4,-91,62,-51,-7,38,54,123,126,-43,-66,-45,-24,-50,53,-97,37,-81,-72,-94,-18,107,90,116,-14,-4,-105,3,-9,-72,98,39,43,66,-60,-10,81,-58,-121,-36,-23,-27,-7,46,7,-17,-107,75,44,125,-58,116,-14,-4,-105,6,-12,-34,53,-41,-84,-79,-26,-45,62,78,-98,95,-110,97,114,-8,-83,81,-20,-107,-115,94,-53,54,27,10,-78,116,-14,-4,-105,4,120,-43,107,56,-102,10,76,-4,87,25,-53,118,76,-19,96,1,82,7,79,47,-55,112,97,-95,-44,-119,-56,-45,113,-16,83,-76,-14,41,-106,89,-70,80,61,95,-107,77,-86,-78,6,-100,43,56,86,112,-84,-31,89,-62,-77,-123,103,10,-50,21,-126,-107,19,-32,-79,-73,48,-5,-1,0,43,-5,127,-1,-60,0,35,16,0,1,4,2,2,2,2,3,0,0,0,0,0,0,0,0,0,1,16,17,49,32,33,2,18,48,65,34,97,64,81,113,-1,-38,0,8,1,1,0,6,63,2,-107,107,44,-78,-53,44,-78,-53,54,64,-115,-67,120,118,-14,104,68,-9,-122,-51,113,55,-60,-98,24,66,-97,19,118,-35,-71,17,-57,30,-55,-114,-50,-53,-28,-20,-66,5,-57,-86,55,-56,-93,88,46,31,109,45,50,-45,-108,-66,-115,-28,-113,-3,-57,109,40,-56,42,50,17,-124,-90,43,-55,69,100,23,26,111,-94,14,-88,46,122,44,-74,-97,108,-66,46,-54,-22,-56,46,82,-76,117,66,-103,89,14,-55,-116,-14,35,-115,8,66,19,-20,87,-21,-54,-119,-30,81,-77,-10,-82,-112,68,108,-111,112,-78,-53,-62,77,-95,-12,75,65,-65,-57,70,-1,-60,0,34,16,0,2,2,1,5,1,1,1,1,1,0,0,0,0,0,0,0,1,17,49,113,16,33,65,81,97,32,-95,48,-127,-111,-1,-38,0,8,1,1,0,1,63,33,68,126,102,-55,-46,60,-121,-112,-14,30,67,-56,121,15,33,-28,21,36,32,1,-49,99,-14,13,59,-19,-48,-60,-106,13,-119,-3,-90,-43,15,-122,-56,-89,5,56,17,57,76,50,9,-84,64,-115,65,-43,66,-20,-27,101,-110,54,4,-23,75,15,-16,86,-103,50,-100,20,-32,71,16,64,-91,2,-88,-78,74,94,-62,-105,-15,25,-64,67,114,-9,-43,-119,99,-35,-28,83,-126,-100,13,-60,-41,98,66,-31,-115,-57,33,73,19,-81,-87,-93,-47,78,10,112,89,-92,78,49,-2,124,127,2,-100,20,-32,-80,-73,-92,73,-46,-124,-91,-62,21,19,-1,0,6,-44,64,-120,-35,-58,-95,-61,-8,41,-63,78,6,-90,43,-79,-67,-82,90,78,-73,6,-25,-95,55,-82,72,23,-74,-87,-67,-108,-32,-89,4,-121,4,98,22,-106,-63,84,48,-87,57,80,-60,-64,84,82,-42,0,83,-126,-100,15,127,-65,-110,-118,74,39,106,-123,-65,3,-46,32,72,-121,5,56,63,17,-77,-31,-85,-84,-87,98,82,35,68,-125,126,-118,73,-28,-108,-32,-4,67,106,46,-23,-119,-89,-76,111,-56,-39,122,70,-60,-88,49,1,73,46,43,-48,-89,5,56,30,93,-4,88,-76,86,77,-94,30,-10,25,-76,-67,10,112,83,-126,-49,-68,106,25,-28,-75,41,-63,-7,5,-123,-9,66,-33,74,61,-61,77,-95,-88,-48,-89,7,-32,55,-72,62,84,-108,-16,-119,-43,65,10,49,-120,70,-94,-118,112,83,-127,42,64,-99,-49,-7,-94,114,-16,77,64,-9,122,-21,69,110,68,50,96,-112,76,101,-77,-48,-89,5,88,24,-44,-59,81,-115,-72,13,-10,-33,13,74,13,117,122,40,-83,-125,19,118,41,-64,-107,114,69,16,-10,103,-77,61,-103,-20,-49,102,123,51,-39,-98,-52,97,54,109,-48,-115,-27,80,-19,36,10,-105,-14,118,-110,78,-97,-1,-38,0,8,1,1,0,0,0,16,-1,0,-23,6,52,-19,-66,13,-64,72,2,40,26,-48,-24,-89,1,-94,73,-109,-60,104,64,2,4,24,-92,-108,-94,37,-128,12,127,-49,-1,-60,0,35,16,1,0,2,2,2,1,4,3,1,0,0,0,0,0,0,0,1,0,17,33,49,16,65,81,32,97,113,-15,48,-127,-95,-111,-1,-38,0,8,1,1,0,1,63,16,113,11,-67,40,-71,-24,19,-18,-89,-35,79,-70,-97,117,62,-22,125,-44,-5,-87,-9,81,-118,99,-13,29,5,-8,124,76,-60,86,-104,-23,-68,65,-11,-78,-96,64,-93,-74,-119,-73,-93,-89,-49,-92,23,66,-4,68,77,-119,-13,17,-76,-113,-76,112,-41,-39,-50,62,32,9,-120,-96,-126,-73,-99,64,55,59,95,32,-87,121,16,48,25,-59,28,111,51,13,3,-52,-94,11,-81,-38,19,-120,-122,78,113,-17,-27,37,-120,-83,-82,-56,-96,10,-121,-52,64,2,-81,68,58,-96,25,-76,-1,0,80,-5,-120,-118,85,-19,-28,90,-29,-78,9,79,55,56,-24,-119,73,-124,56,37,29,-80,26,-81,-64,75,14,40,-63,-30,33,74,-24,122,-78,-25,43,-100,127,-24,120,36,18,-123,-125,20,43,-114,-97,-119,-57,-2,-122,99,109,-71,97,81,19,27,-17,16,2,-41,80,-63,-83,99,-61,81,76,32,-22,35,5,39,94,-89,29,16,45,112,-124,103,19,85,85,-37,12,64,-102,-4,-61,87,-66,-66,98,-7,3,-29,-87,91,-15,50,-76,0,28,-42,-126,-107,-33,56,-9,33,-107,99,-81,-64,113,99,-118,-9,51,-42,18,-122,-103,33,-62,-32,-37,7,46,-16,28,110,43,27,-85,-25,60,91,-68,-52,-33,-95,66,-31,86,77,-128,91,33,35,-83,-57,-114,30,-93,-69,96,-72,-80,-104,-25,25,-112,-72,23,92,-125,-104,61,-61,54,6,-21,-124,91,18,-36,70,23,-43,-70,-32,-64,85,-22,58,109,-85,124,-29,42,-97,53,-50,-27,-26,-104,26,52,-24,-105,23,-86,19,42,-67,30,-16,17,-5,102,-122,-7,28,116,-89,82,-119,109,95,67,86,-89,26,-10,-32,-11,1,90,55,40,38,-84,69,45,106,-37,-24,113,-1,0,-95,-11,-115,-91,89,-53,-18,120,-34,-93,-48,-31,-80,123,-116,97,84,-6,-111,-21,63,-40,73,-121,114,119,5,4,97,-10,-105,-124,-68,60,-71,-124,86,-116,-117,105,-24,5,104,45,-124,69,-74,-91,-71,81,-117,59,-103,-120,46,-31,81,-86,-18,5,8,19,93,-6,28,122,-29,105,105,96,-108,81,-49,12,53,-119,-107,88,51,69,117,28,30,100,55,3,9,-116,62,37,109,-76,60,-72,44,-4,-56,57,77,-51,116,-3,-64,-6,16,52,80,-5,-51,-13,-80,18,46,51,12,67,-93,-88,-26,40,-45,-116,120,32,85,80,-100,23,-25,-65,-61,17,17,17,17,117,95,-79,-98,-40,32,-74,-122,8,-84,-111,-66,-90,127,17,-8,-124,8,29,-100,127,-1,-39,};
	public final byte [] uno2 = {-1,-40,-1,-32,0,16,74,70,73,70,0,1,1,2,0,37,0,37,0,0,-1,-37,0,67,0,40,28,30,35,30,25,40,35,33,35,45,43,40,48,60,100,65,60,55,55,60,123,88,93,73,100,-111,-128,-103,-106,-113,-128,-116,-118,-96,-76,-26,-61,-96,-86,-38,-83,-118,-116,-56,-1,-53,-38,-18,-11,-1,-1,-1,-101,-63,-1,-1,-1,-6,-1,-26,-3,-1,-8,-1,-62,0,11,8,0,-101,0,100,1,1,17,0,-1,-60,0,25,0,0,3,1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,3,4,5,2,1,-1,-38,0,8,1,1,0,0,0,1,101,28,0,1,-41,-103,-70,121,-114,-97,-128,3,-35,23,-15,-98,-58,66,57,-32,113,127,25,-105,68,-69,56,-104,25,-94,-50,51,92,-70,37,88,6,-123,60,100,-106,33,64,5,87,-15,-110,-2,-109,119,83,74,21,-35,-58,85,-80,-24,-5,61,-7,106,47,-85,-119,18,-85,-90,-29,95,49,38,-93,120,-98,16,110,-126,-13,-122,104,52,-50,-100,-93,66,25,66,-33,45,50,120,53,-69,-32,-123,84,-13,127,57,0,-32,23,100,116,95,-58,72,0,85,-62,42,-65,-52,-113,0,30,-55,74,111,51,-89,2,-81,103,-73,-107,-33,-60,-53,83,28,-124,50,-87,-39,127,57,44,98,-44,29,92,-122,89,-54,-46,0,7,-75,75,54,-96,0,1,44,31,-1,-60,0,34,16,0,2,2,2,3,0,2,3,1,0,0,0,0,0,0,0,1,2,0,3,17,18,16,19,50,32,49,33,34,35,48,-1,-38,0,8,1,1,0,1,5,2,-82,-67,120,-39,102,-53,54,89,-78,-51,-106,108,-77,101,-101,44,-56,48,-88,104,-23,-85,31,-64,102,46,69,39,12,-91,127,-62,-89,-40,71,-13,89,1,-103,-53,61,-1,0,92,45,101,-90,-75,-92,-19,81,59,84,-61,90,-80,-89,-36,127,42,-69,19,-4,-61,-20,120,90,-62,-121,-73,63,4,109,72,3,49,-4,-85,106,123,-102,53,-123,-30,-88,-84,59,-106,63,26,15,-21,31,-49,21,-88,1,-37,99,-14,-93,-44,127,50,-91,-40,-38,-7,42,-69,17,72,-99,75,44,-85,31,10,62,-29,-7,31,-110,-33,-51,37,43,-123,-79,-12,-117,113,-52,-80,97,-8,-93,-52,127,52,-84,-79,-74,104,-73,0,44,109,-103,70,76,-73,-33,21,-116,36,111,45,-6,85,-54,33,104,-88,22,89,102,-68,-96,-53,103,-6,113,113,-3,-72,-82,-67,-89,-48,123,115,-16,-88,106,-75,28,-39,-61,122,-31,70,22,106,-77,85,-105,12,50,-115,-115,-89,2,-113,81,-68,-13,-38,-45,-75,-89,107,78,-42,-124,-106,-118,58,-48,-100,-102,61,71,-13,-13,-87,37,-113,-79,-108,122,-121,-14,62,85,-90,-46,-57,-26,-113,92,90,-72,111,-126,85,30,-56,-89,12,-113,-79,-71,-122,40,-11,27,-54,-80,117,106,76,-47,-96,-91,-90,18,-72,-10,22,-31,6,-51,99,105,10,29,104,-11,27,-49,-44,22,48,-99,-51,13,-116,126,10,117,57,71,-106,-66,101,3,-123,96,-63,-85,13,58,39,68,-24,-99,19,-94,116,78,-119,-47,5,51,-22,61,-72,106,-55,13,-2,87,49,28,127,-1,-60,0,34,16,0,1,4,2,3,0,2,3,0,0,0,0,0,0,0,0,0,1,16,17,49,32,33,2,18,48,34,97,50,65,81,-1,-38,0,8,1,1,0,6,63,2,-35,-75,-106,89,101,-106,89,101,-101,-57,126,74,74,-97,17,63,-72,108,-46,27,66,120,58,-112,124,80,-107,105,-28,66,99,-39,-107,-32,-98,94,42,-3,-71,121,43,-62,81,-93,101,18,-103,43,66,91,75,109,-105,21,59,102,-82,-116,-92,101,9,120,67,-61,-49,-23,-95,48,-20,-94,-85,-82,52,83,65,-43,29,112,-78,-53,44,-39,-39,73,117,-16,-107,62,-68,-2,-114,-68,124,-25,-111,-41,-119,39,-30,67,-87,-41,-111,-94,-101,-19,-32,-21,-60,-20,-22,-42,-41,-124,-101,33,9,109,53,-106,89,101,-106,89,102,-43,-95,61,52,-33,-1,-60,0,34,16,0,2,1,3,5,1,1,1,1,0,0,0,0,0,0,0,0,1,17,16,33,49,32,65,81,97,-95,113,48,-111,-63,-1,-38,0,8,1,1,0,1,63,33,74,72,54,-106,78,-95,-44,58,-121,80,-22,29,67,-88,117,5,-120,-126,-120,65,-48,44,13,35,-32,-1,0,26,53,38,-31,-115,99,74,8,19,107,12,-80,60,-94,23,7,-120,-102,-94,44,-100,16,-67,87,84,-115,-58,93,22,-88,-8,-118,-45,39,79,16,-27,-95,8,37,100,-120,93,97,-77,98,-76,54,101,-127,-88,-9,84,-15,15,-103,29,72,85,36,111,24,-72,-79,-58,-87,97,-59,60,90,55,63,-81,109,111,114,-89,-118,-105,87,-124,112,80,100,1,85,-71,-47,65,-54,-76,45,-18,-98,33,32,72,117,-69,40,-114,83,35,91,44,-28,-120,-54,16,-126,-86,92,-23,-30,47,54,-40,-112,116,69,80,-20,93,-106,6,37,33,96,105,-118,-79,71,93,100,-103,122,26,-16,-116,126,121,20,-128,110,92,-70,67,-88,63,-127,86,94,37,89,-110,22,-30,72,115,97,90,20,-51,17,-113,44,-18,-117,34,-44,-112,-46,106,25,-44,58,-126,19,10,44,70,-123,-84,-78,-4,-89,-101,-16,73,39,-109,115,38,-78,49,-50,99,47,-54,120,-65,9,56,-47,104,88,83,47,-54,44,-88,106,28,106,107,75,-80,-8,-60,62,25,15,-125,47,-54,-34,-74,122,102,-38,66,-93,96,-114,-124,-106,101,103,34,48,-28,-53,-14,-98,99,126,2,47,122,47,68,-122,51,98,4,-73,34,-53,-123,72,86,33,-92,65,126,51,47,-54,44,-95,14,91,-70,35,-87,105,36,40,-49,-55,9,85,-115,25,105,-112,11,-2,31,-30,-86,-86,-76,-14,9,36,-123,-127,-10,-111,33,39,-97,-51,74,77,19,79,-1,-38,0,8,1,1,0,0,0,16,63,-50,-122,40,71,-66,89,-63,-56,14,4,59,-125,-122,60,36,-13,67,55,85,-8,-64,36,0,48,-107,-20,105,-25,80,13,-1,0,-1,0,-1,-60,0,37,16,1,0,2,1,4,2,2,2,3,1,0,0,0,0,0,0,1,0,17,33,16,49,65,81,32,97,-16,-15,113,-127,48,-111,-95,-79,-1,-38,0,8,1,1,0,1,63,16,32,98,-65,-56,13,-96,61,-49,-70,-97,117,62,-22,125,-44,-5,-87,-9,83,-18,-89,-35,77,-6,126,-27,81,49,17,46,-31,-88,110,-20,46,86,-123,-91,-64,-108,-109,-64,74,-87,87,-77,-33,-120,-99,-123,-4,69,27,-119,-7,-114,-118,9,-44,103,-53,-68,-9,20,-36,63,-109,70,37,64,12,75,10,4,-86,-16,49,-101,-81,99,0,1,-100,66,-45,127,-119,68,9,-6,-118,-101,39,16,-100,68,50,106,-40,-120,-67,-40,4,-41,-35,-88,-86,-27,-9,1,90,11,101,-41,59,8,98,62,-2,98,-85,107,110,-89,83,45,-56,73,123,53,102,-58,42,86,116,-128,32,-33,21,48,-39,102,8,-127,85,-61,-55,102,-37,87,-117,42,-94,-85,3,20,43,-114,-49,53,45,97,60,25,68,57,-39,-74,30,-114,96,114,-66,-34,-90,-10,105,101,-50,6,-25,94,10,-15,-62,-75,101,-36,75,80,-45,96,-53,-52,-34,2,-89,108,97,13,-19,122,-123,38,99,-3,66,-67,9,54,-40,-69,-44,-54,121,93,88,29,-121,108,94,52,-63,-94,126,-63,80,-22,-112,20,64,114,-37,-126,-127,-47,5,-117,26,111,19,0,106,-12,54,7,36,123,32,-13,-31,-113,20,-18,-80,28,125,-108,80,-94,-65,-56,-120,-106,-69,-24,-63,88,-69,96,80,116,80,74,75,35,117,28,107,-43,39,-5,-98,-96,35,-28,-10,-14,-59,86,-42,-35,92,74,-58,35,-91,-55,-94,-48,-81,16,12,108,116,11,7,108,-22,90,80,-117,30,39,-43,79,-86,-123,-124,58,68,19,-105,48,26,-94,-77,-82,52,67,12,111,-96,-46,39,16,16,13,-98,27,99,105,-63,12,8,-19,17,-40,-54,-1,0,30,48,-36,-21,49,124,-52,-5,-47,-17,-61,61,-18,70,117,-72,-41,-106,86,-122,111,-71,-118,-118,24,83,-103,-11,-47,34,-44,126,-68,49,-44,-100,-25,-128,43,65,108,64,37,124,60,-61,49,3,-123,37,-77,-125,-122,37,68,-27,72,-92,-123,-17,70,-38,-29,68,55,-127,-40,118,-116,-28,107,105,-37,81,-99,-89,-36,49,-46,-124,84,-11,-102,26,74,29,-27,72,42,-34,2,-99,28,-5,-41,16,-59,-87,-120,40,-94,9,86,9,58,116,-127,105,65,-22,42,-74,-74,-22,-62,112,-54,-59,103,100,8,-67,-47,-59,4,28,30,-12,4,-42,127,-55,112,-113,105,62,85,62,85,62,85,62,85,62,85,62,85,62,85,62,85,6,-88,14,33,-111,1,-60,72,8,55,88,-18,-127,82,127,27,72,27,-86,42,-74,-17,63,-1,-39,};
	public final byte [] uno1 = {-1,-40,-1,-32,0,16,74,70,73,70,0,1,1,2,0,37,0,37,0,0,-1,-37,0,67,0,80,55,60,70,60,50,80,70,65,70,90,85,80,95,120,-56,-126,120,110,110,120,-11,-81,-71,-111,-56,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-62,0,11,8,0,-102,0,100,1,1,17,0,-1,-60,0,24,0,0,3,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,2,3,4,1,-1,-38,0,8,1,1,0,0,0,1,106,112,0,3,-103,-75,-27,-22,112,14,-122,-103,95,38,-100,-22,61,56,29,91,-29,-68,-71,94,72,31,68,111,-115,-42,-46,80,13,51,-66,50,-45,80,3,66,95,29,9,-106,68,13,9,124,-106,-125,104,124,-14,13,51,-68,17,89,-19,25,29,-41,11,-58,32,106,-108,-121,-68,-76,102,-104,26,-91,34,-30,-24,-56,-96,106,-108,-99,-103,111,-116,1,-72,90,54,91,-29,0,11,34,93,111,-109,-128,21,-30,114,-21,-93,60,-125,-74,73,-34,23,91,-60,94,-76,-108,-47,42,45,-14,119,-85,-64,45,59,37,-124,0,3,-76,-53,-51,96,0,4,97,-1,-60,0,33,16,0,2,3,0,2,3,1,1,1,1,0,0,0,0,0,0,0,1,2,17,49,16,18,3,32,33,50,34,48,67,-1,-38,0,8,1,1,0,1,5,2,-116,120,-76,90,45,22,-117,69,-94,-47,104,-79,-85,31,-58,55,103,71,-2,49,118,-89,-81,8,-69,30,-16,-93,103,-14,-114,-57,100,56,-98,50,122,-16,-15,-113,74,-95,-53,-46,46,-124,79,94,9,-41,9,117,27,-65,104,100,-11,-25,9,80,-35,-5,-61,39,-81,8,-85,36,-17,-107,2,74,-67,33,-109,-41,-125,-2,98,37,109,70,-72,-98,-13,12,-98,-68,-128,-35,-79,124,59,-79,125,83,-34,86,79,94,100,125,35,-7,-98,-15,29,91,61,39,-66,-111,-4,-49,120,95,35,2,122,61,-12,-113,-26,122,69,91,-101,60,100,-11,-25,-83,-13,-7,-119,-29,39,-81,61,-30,-122,-17,-113,25,61,-9,-116,73,59,-25,-58,79,73,-81,85,26,37,43,-29,-2,103,-116,-98,-68,82,28,14,-84,-24,90,-120,-35,-14,-66,-63,-86,60,100,-11,-15,103,102,95,-86,-107,38,-20,-126,-7,61,78,-58,-84,-24,-114,-120,-24,-114,-120,-24,-114,-120,-24,-114,-120,-24,-72,-109,-74,-73,-4,-68,-100,127,-1,-60,0,28,16,0,1,4,3,1,0,0,0,0,0,0,0,0,0,0,0,33,0,1,17,48,49,64,80,16,-1,-38,0,8,1,1,0,6,63,2,-31,97,97,10,13,-110,-10,75,-24,26,99,-71,27,18,-5,6,-120,116,60,40,115,-1,0,-1,-60,0,31,16,0,2,2,2,3,1,1,1,0,0,0,0,0,0,0,0,0,1,17,49,16,65,32,33,97,48,81,-111,-1,-38,0,8,1,1,0,1,63,33,-127,75,-79,-72,61,-113,99,-40,-10,61,-113,99,-40,-10,18,61,-120,78,-60,-24,31,72,115,-119,-120,106,28,115,-84,-121,120,-100,57,63,25,126,88,34,-34,-39,21,66,75,8,106,78,-40,-17,-58,-59,-8,74,-78,94,-21,-91,-62,87,-126,41,-107,-68,119,-31,-11,45,-120,77,-125,-37,-109,78,61,-7,82,19,-37,-29,-9,-30,116,-24,-99,10,-77,50,18,33,114,-5,-53,27,-47,-114,-96,71,38,-104,-9,-119,-36,-26,26,83,-119,-5,56,-23,8,-79,-36,58,-10,-8,-43,-61,89,45,47,-99,-87,-7,-58,-82,29,-29,124,-10,113,-85,-122,26,26,54,-57,103,41,45,-115,-51,-29,-5,99,108,119,-4,16,-112,76,-58,-40,-33,104,125,62,91,29,21,-107,16,-30,113,-74,126,-55,85,-62,-59,36,-102,10,-72,54,-57,113,50,-64,60,-59,-6,65,11,102,54,-39,78,-127,-9,54,-58,-110,-96,106,28,9,-106,-49,97,-77,-33,21,-64,57,-56,-91,-115,104,38,-33,26,-86,-92,-122,86,6,-65,-98,-104,-1,-38,0,8,1,1,0,0,0,16,0,17,120,-109,-42,-117,-75,-1,0,-73,-3,-1,0,-30,127,119,-53,-17,-33,118,123,87,-60,-65,-42,-5,-89,53,11,-114,-1,0,-49,-7,-1,0,-1,0,-1,-60,0,34,16,1,0,2,1,4,3,1,1,1,1,0,0,0,0,0,0,1,0,17,49,16,33,97,113,32,65,81,-95,48,-111,-127,-1,-38,0,8,1,1,0,1,63,16,50,11,95,-112,5,-88,78,12,-32,-50,12,-32,-50,12,-32,-50,12,-32,-60,-88,12,-83,9,121,-15,21,-117,-22,112,-97,68,0,-20,92,-80,94,-68,65,112,68,76,-63,85,-115,49,-51,114,65,67,-87,-8,37,78,85,55,88,84,-3,26,-17,-8,62,-80,38,-24,14,105,-125,-24,-125,-20,-68,-41,-60,-74,-42,98,-22,126,13,95,-47,1,90,51,47,-73,-4,-101,15,-124,67,-1,0,68,18,-109,23,83,-16,105,105,67,120,-83,-2,-41,76,-117,19,-113,71,-106,83,-44,-59,-44,-4,26,-1,0,-109,9,98,113,-24,-13,-51,-36,-59,-44,-4,26,110,44,39,-64,26,-18,41,47,-44,51,-8,120,102,-18,98,-22,126,8,10,3,44,32,25,103,74,43,85,-51,-6,-83,-6,-23,-117,-81,10,-81,-10,98,-22,126,9,122,102,-60,101,116,81,25,39,1,28,86,89,-117,-83,66,-38,-99,65,49,117,63,20,-38,-31,-28,76,93,107,77,-16,-34,13,92,27,76,93,68,18,-103,119,-53,-55,49,117,-82,-19,-27,-60,118,-73,-36,-59,-44,-60,118,-36,-7,76,93,105,70,122,51,5,76,26,28,93,71,76,124,-103,-15,5,66,8,-118,-43,-64,86,-116,-62,-73,51,-118,-83,-71,-47,-59,-44,-4,31,-61,26,-85,17,-35,113,-21,87,23,80,88,125,32,-80,-8,-7,34,-29,31,101,-81,66,122,-122,-66,-22,-30,-22,40,22,-30,81,91,119,-128,42,-126,-39,-8,10,47,-95,-30,56,-70,-97,-118,22,68,-11,61,-114,-51,3,100,56,70,-128,114,70,-9,109,-13,85,17,-106,82,88,-33,71,23,82,-57,-24,-116,-56,-92,-104,36,69,74,-116,-110,124,74,-95,-78,90,92,122,34,34,42,-26,46,-95,54,-9,-10,76,123,127,-77,-111,-100,-116,-28,103,35,57,25,-56,-50,70,114,50,-31,-69,-36,80,91,-80,66,68,-60,105,67,-17,-7,-92,5,-23,-1,-39,};
	public final byte [] uno0 = {-1,-40,-1,-32,0,16,74,70,73,70,0,1,1,2,0,37,0,37,0,0,-1,-37,0,67,0,80,55,60,70,60,50,80,70,65,70,90,85,80,95,120,-56,-126,120,110,110,120,-11,-81,-71,-111,-56,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-62,0,11,8,0,-102,0,100,1,1,17,0,-1,-60,0,24,0,0,3,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,2,3,4,1,-1,-38,0,8,1,1,0,0,0,1,45,-64,0,3,46,-68,-51,53,3,-95,-95,-102,9,72,15,78,29,44,65,94,53,-28,-8,54,-82,-103,40,-108,-102,0,105,-95,-113,-106,68,0,47,99,27,52,-19,73,-57,-123,-20,99,-76,105,124,-9,-116,-115,21,51,-86,104,51,-24,51,-102,-40,-106,115,65,-97,65,-99,-75,116,-124,75,83,46,-107,-123,-53,25,80,109,74,-39,-71,78,-40,-57,-64,-21,-86,-34,54,-79,-113,-128,5,-111,47,99,34,-128,83,-77,45,98,17,14,-41,-119,99,-106,32,47,90,105,78,-65,44,99,-17,120,-95,-37,-93,-43,58,-128,0,118,-103,-25,-80,0,0,-116,63,-1,-60,0,33,16,0,2,2,2,2,3,1,1,1,0,0,0,0,0,0,0,1,2,0,17,16,18,32,49,3,33,50,48,65,34,-1,-38,0,8,1,1,0,1,5,2,85,-59,-119,98,88,-106,37,-119,98,88,-106,48,69,-62,40,-62,73,58,66,43,-16,67,112,-117,-62,125,19,-19,-66,112,22,-27,40,-101,9,-80,-123,110,38,83,-24,-125,109,-13,2,-44,45,-64,26,-31,-3,-34,19,112,13,97,55,-55,58,-32,-94,19,124,-4,124,20,89,99,-128,-110,-124,41,-61,-57,-61,-27,98,15,100,-48,-40,-64,108,56,-54,117,-123,30,-40,-39,-119,31,-88,-99,63,88,94,-79,-46,-31,58,126,-94,116,-3,69,-17,-5,-121,-49,-114,63,81,58,124,47,-96,-103,110,-16,13,25,-88,-61,27,32,89,99,60,124,-18,108,101,-34,7,-7,19,-57,-7,40,-116,111,30,60,-98,-8,-86,-58,105,88,-15,-27,-57,16,-80,-76,30,-53,26,-115,-19,124,121,13,10,77,76,-46,122,88,77,-31,126,-97,-77,-15,-29,-55,-105,54,50,-8,-20,12,102,-72,-104,83,-24,-117,-102,9,-96,-102,9,-96,-102,9,-96,-102,9,-96,-102,-116,51,123,-4,-33,31,-1,-60,0,29,16,0,1,4,3,1,1,0,0,0,0,0,0,0,0,0,0,17,0,1,33,48,16,32,64,49,80,-1,-38,0,8,1,1,0,6,63,2,-8,94,47,20,81,42,43,47,97,123,70,39,17,-11,-54,123,7,65,-76,54,-57,89,81,-109,-96,117,28,-109,-35,-1,-60,0,33,16,0,2,2,2,3,0,3,1,1,0,0,0,0,0,0,0,0,1,17,49,16,33,32,65,97,48,81,113,-127,-111,-1,-38,0,8,1,1,0,1,63,33,-18,120,-12,61,-113,99,-40,-10,61,-113,99,-40,77,62,-60,38,-51,65,72,-127,31,-79,-19,-16,64,-40,-101,14,-117,7,79,102,-21,-105,14,-51,-111,80,-102,-62,18,79,124,58,44,58,-45,84,88,66,73,-97,90,46,19,36,81,107,-68,58,38,36,-113,-56,-57,20,-128,-10,-28,-45,-117,-95,-34,35,65,59,-97,108,58,29,-97,-128,117,-85,51,-56,84,112,118,-61,-93,-79,-43,-37,-60,-46,54,6,-23,55,6,-23,-29,-70,38,-97,-42,97,116,-33,63,-46,23,14,-121,-2,-33,13,-6,74,-119,-20,-78,-5,-116,-75,-119,56,-92,23,-46,88,-35,97,-27,-28,-46,-7,-36,9,-90,-115,-108,54,-110,54,6,-96,-43,3,-74,93,-16,76,-87,-98,-29,107,60,38,-34,-58,-27,-99,-80,-24,119,-49,-68,74,-13,29,-78,-80,-36,-92,-37,-93,-16,4,-49,-84,118,-49,67,-116,123,61,84,22,20,75,-124,-123,7,108,58,63,32,-109,99,-52,77,-34,-113,-20,-57,-31,86,99,-74,82,24,-103,118,123,18,-5,-32,-100,57,21,33,65,80,-70,111,19,122,19,101,-16,-107,85,37,20,49,-24,45,49,87,-60,-11,-113,-1,-38,0,8,1,1,0,0,0,16,0,41,-8,79,-10,109,-46,-66,-101,-12,-35,-83,63,119,-23,-1,0,110,-9,126,-105,-52,-97,-76,-1,0,55,57,45,106,-68,95,-7,127,-1,0,-1,-60,0,32,16,1,0,2,1,5,1,1,1,1,0,0,0,0,0,0,0,1,0,17,49,16,33,65,97,113,81,32,-127,48,-1,-38,0,8,1,1,0,1,63,16,42,14,-2,18,-64,-33,105,-43,-99,25,-47,-99,25,-47,-99,25,-47,-99,25,-124,12,-96,8,-19,-61,-120,5,6,8,-116,93,94,-60,48,-70,-78,-124,-29,-121,-14,11,-126,34,100,-126,-104,106,42,-103,17,107,22,-52,-98,105,46,22,-39,-118,-53,-99,119,124,31,102,3,-72,-72,-43,80,77,16,75,32,121,10,37,-50,-103,60,-46,90,85,-83,-52,54,-39,32,43,70,102,49,87,17,96,21,91,119,117,83,24,-123,117,52,-55,-28,19,37,35,5,91,-17,-101,-25,-8,78,66,112,75,19,-114,15,-42,-39,-13,76,-98,76,-98,-24,8,91,24,-116,-19,-19,-57,-8,-71,60,-103,61,-103,-20,51,46,109,-95,1,90,51,6,-121,115,-14,109,85,101,-125,-105,-56,-120,-45,-97,-53,-109,-56,-106,-61,-106,32,4,22,-37,-128,-117,-120,72,-72,-100,-105,-14,1,9,65,38,-50,-94,-105,-70,100,-14,92,44,40,-85,-16,-40,-45,-38,51,23,-70,102,-10,98,-9,90,38,-103,60,-101,-76,119,-43,-97,-39,-117,-35,51,123,49,123,-91,18,10,14,53,-68,-100,26,-123,-114,98,-75,113,-66,-113,-71,-50,-72,7,-14,81,16,-13,-83,-127,-5,-83,23,14,101,-120,108,102,118,82,-60,-20,18,-37,-121,17,8,66,7,22,117,93,-122,59,107,-9,-15,-101,13,6,120,116,7,121,68,69,-53,-85,-109,-55,-109,-33,-39,117,-111,58,-40,-4,57,-108,42,-83,-1,0,95,46,33,36,1,88,-104,-120,-46,87,-31,-33,-32,115,-8,5,104,-35,-128,49,31,37,-27,85,6,-122,34,-111,-86,0,82,-99,92,-98,64,47,118,-48,5,-73,-42,-127,-37,-80,75,-57,5,-9,118,-7,-84,-57,-28,-57,-26,-85,-72,-60,113,-5,48,72,-47,47,-107,-8,99,25,32,-105,66,74,85,84,56,-116,58,9,11,-77,66,59,-103,-40,-50,-58,118,51,-79,-99,-116,-20,103,99,30,-70,-65,102,-61,-31,24,-35,2,52,81,-83,-26,15,63,-55,-128,13,14,-97,-1,-39,};
}

//the back of the cards, reverse, skip and +2 cards
class ImagesOther{
	public static final byte [] unoB = {-1,-40,-1,-32,0,16,74,70,73,70,0,1,1,2,0,37,0,37,0,0,-1,-37,0,67,0,2,1,1,2,1,1,2,2,2,2,2,2,2,2,3,5,3,3,3,3,3,6,4,4,3,5,7,6,7,7,7,6,7,7,8,9,11,9,8,8,10,8,7,7,10,13,10,10,11,12,12,12,12,7,9,14,15,13,12,14,11,12,12,12,-1,-62,0,11,8,0,-102,0,100,1,1,17,0,-1,-60,0,29,0,0,2,3,1,1,1,1,1,0,0,0,0,0,0,0,0,0,8,5,6,7,9,4,3,1,2,-1,-38,0,8,1,1,0,0,0,1,-70,-76,17,0,0,72,66,-15,-17,-79,-36,114,-89,0,1,-5,-43,109,90,-95,-56,16,15,-66,-87,123,-8,-28,46,91,-5,-126,114,0,11,99,31,54,30,93,61,-33,-63,57,0,23,54,103,-44,0,-59,-77,56,39,32,11,67,73,-22,0,25,38,75,4,-28,7,-35,-82,-80,-121,-41,-47,83,-99,104,88,92,15,-112,59,118,-47,37,45,-105,-1,0,84,72,111,67,-12,-50,96,124,-106,117,107,84,89,-52,18,-3,60,-60,-91,111,-125,-101,-103,115,55,114,86,-25,-67,126,41,106,-43,-14,49,-40,104,-24,60,-119,-37,35,105,-98,90,20,-3,-78,78,-3,-95,62,-39,-57,56,41,-38,-18,119,-103,55,-80,115,80,-22,-5,-106,-2,-25,41,-102,-61,36,-63,-53,66,-4,-116,27,34,127,95,-36,-27,33,-110,-6,68,-127,-110,96,-125,-6,-2,-27,-55,96,1,-100,46,127,-64,-2,-65,-72,26,81,111,15,-52,127,13,-2,1,-3,127,112,62,74,-17,118,-102,-122,67,84,0,127,95,-36,-53,-117,94,112,0,14,-118,-78,-44,-103,124,-68,0,15,67,47,-57,-68,-25,-67,31,-96,0,28,-101,90,127,-1,-60,0,41,16,0,2,1,3,4,2,2,1,4,3,0,0,0,0,0,0,5,6,4,2,3,7,0,1,8,16,23,55,24,32,21,17,18,20,48,22,36,49,-1,-38,0,8,1,1,0,1,5,2,-29,-57,30,4,-115,82,34,-64,25,38,47,-107,-43,-75,-27,117,109,121,93,91,94,87,86,-41,-107,-43,-75,-27,117,109,121,93,91,94,87,86,-48,-52,-126,4,-36,-41,76,72,-66,-5,21,-3,58,-6,3,-117,-29,92,92,84,-128,-10,-10,71,34,-79,-1,0,70,-37,-2,-35,-8,-81,-107,38,100,37,7,60,58,-71,-112,74,114,-117,-47,-97,88,-47,-82,76,-68,47,19,-56,-109,22,38,51,21,30,-39,12,96,54,85,-74,-124,-87,43,85,-16,111,-82,80,-6,51,-24,-80,-95,37,-98,-32,69,-24,-85,-47,-5,-103,14,-47,8,-68,65,9,82,-7,-115,114,-121,-47,-99,-89,-89,-35,102,-107,10,21,-95,-47,126,-68,122,-113,69,50,53,-54,31,70,116,-92,-73,91,41,72,80,-83,14,-117,-10,-29,-25,92,-95,-12,102,-93,70,-82,100,-107,-128,52,46,8,-18,-51,-118,-28,94,54,78,10,118,-82,63,-106,-38,80,-74,-79,-122,43,-63,-93,-81,-120,-99,-82,81,122,51,88,-79,115,-10,83,-95,65,-27,27,-105,37,52,-124,3,36,50,-36,95,-50,73,-51,37,64,8,-117,14,-28,72,-52,105,13,107,-111,0,21,-36,-60,94,51,48,89,48,3,92,-95,-12,96,65,85,-101,43,13,74,100,105,68,-33,68,12,46,105,-36,-93,33,101,70,-62,40,-105,88,-89,87,105,-68,-49,-23,36,100,73,-10,-58,-105,17,3,104,108,-87,52,111,81,14,40,-35,-38,-31,-115,102,112,113,88,113,-120,103,-19,-25,-50,64,57,118,-24,22,-128,-2,59,-55,12,63,-21,-48,90,101,113,-121,-58,-99,13,-94,40,-24,123,-64,-118,40,-67,35,100,-60,38,64,100,97,67,119,-34,95,27,102,-59,-121,-42,82,96,-39,101,36,-56,-117,-104,-39,-3,14,78,-47,29,115,-14,-19,85,11,15,119,-14,-128,87,-18,127,48,79,-8,-68,-6,35,6,115,-44,68,25,25,46,-111,-8,70,-11,23,-22,-104,39,18,-86,-16,-106,69,115,36,-21,47,122,-13,57,5,-4,-128,112,-14,-83,-98,8,-48,-5,113,-67,22,-59,-102,71,-58,28,86,-14,-7,108,107,104,-118,18,-36,-27,-75,-94,-60,-56,-106,-72,70,-101,-73,41,-77,109,-67,-66,-21,60,-82,13,-11,-105,-67,121,26,-44,50,-61,-39,-112,78,-29,73,43,53,-79,57,73,87,17,-29,-8,123,28,-90,41,43,-41,-85,-109,119,-89,-57,-49,-26,-11,-63,-66,-78,-9,-81,53,0,-60,-79,90,-100,126,113,59,95,76,-128,-15,80,-35,-6,-32,-33,89,-86,109,-95,-8,-45,-20,-16,-15,72,11,119,46,85,122,-25,92,27,-21,-108,94,-116,73,61,65,-64,95,70,-4,-109,77,-115,-82,92,-86,-11,-50,-8,55,-41,40,-67,22,28,-59,-16,83,-125,100,-56,4,105,-72,-46,54,-43,-78,121,78,4,77,-104,92,102,48,-36,-6,-16,111,-84,-48,-97,125,-13,24,75,-119,118,4,-81,-23,-31,48,9,-112,67,-70,-26,69,-52,120,83,-113,25,-118,30,71,82,-55,60,125,92,-55,-13,-66,22,-85,-21,-31,106,-66,-66,22,-85,-21,-31,106,-66,-66,22,-85,-21,-31,106,-66,-66,22,-85,-21,-31,106,-66,-94,112,-51,86,52,-94,-123,7,35,46,102,-116,-105,86,85,123,18,86,80,66,27,127,-49,-23,-27,59,4,-7,25,107,95,-1,-60,0,68,16,0,2,1,2,4,1,6,7,13,8,2,3,0,0,0,0,1,2,3,4,17,0,5,18,33,49,16,19,34,65,81,97,6,32,35,50,113,-125,-77,20,51,52,54,66,-127,-124,-111,-109,-108,-61,-47,-45,36,48,67,82,-95,-63,-31,-16,21,-79,37,-93,-62,-1,-38,0,8,1,1,0,6,63,2,-89,-51,-13,122,122,76,-38,-77,54,-127,102,68,-102,49,36,84,-15,-80,12,0,7,-27,112,-71,-7,-121,89,106,104,42,-21,114,-36,-90,29,58,32,73,101,72,23,74,-37,101,6,-37,13,-72,99,-29,38,67,-9,-8,-65,60,124,100,-56,126,-1,0,23,-25,-113,-116,-103,15,-33,-30,-4,-15,-15,-109,33,-5,-4,95,-98,62,50,100,63,127,-117,-13,-57,-58,76,-121,-17,-15,126,120,-8,-55,-112,-3,-2,47,-49,31,25,50,31,-65,-59,-7,-31,41,-88,-77,-84,-90,-82,-90,79,54,40,106,-29,119,110,-66,0,-30,-83,107,-14,-54,67,81,88,-70,90,-83,34,85,-87,82,56,16,-10,-66,-42,31,85,-72,99,48,-54,42,14,-89,-94,-105,72,125,-121,56,-68,85,-72,-101,93,72,54,-22,-66,42,-77,33,73,-86,-101,45,-119,82,58,120,108,-125,-120,68,94,-27,-71,30,-127,-43,-119,-77,60,-50,110,114,105,54,85,30,100,43,-44,-118,58,-128,-1,0,119,-3,-51,-58,-40,-87,-91,-52,100,122,-118,-20,-99,-107,12,-19,-58,88,-40,29,26,-113,91,116,91,127,71,19,124,37,102,111,-105,10,-70,-104,-29,16,-85,-13,-46,37,-106,-28,-37,-94,71,89,56,-50,-3,71,-73,-113,-58,17,-59,27,-54,-19,-63,84,92,-100,7,-87,-88,20,-46,19,-17,97,117,-19,-23,-66,44,-15,73,57,-65,-100,-14,27,-1,0,75,99,-56,-119,41,-104,3,-70,-74,-81,-82,-8,-43,-68,-12,-42,-9,-43,94,30,-98,-52,120,77,-12,95,-59,-28,-50,-3,71,-73,-113,-59,60,-35,-93,-127,15,74,70,-31,-24,29,-89,28,-35,52,122,117,121,-52,119,103,-12,-8,-113,12,-56,36,-118,65,102,7,30,22,83,-101,-23,6,-104,-58,79,-54,95,43,110,76,-17,-44,123,120,-4,75,-101,-57,75,25,-23,-65,111,112,-17,-62,67,10,8,-30,-116,88,40,-15,-77,-103,109,-45,117,-127,73,-18,28,-27,-65,-20,-14,103,126,-93,-37,-57,-54,-79,116,-42,4,-34,87,31,36,127,-100,36,48,-96,-114,40,-59,-108,15,31,54,-11,95,-3,-14,103,126,-93,-37,-57,-56,-111,70,53,73,35,5,81,-38,112,-112,46,-17,-25,72,111,-59,-70,-4,69,-115,17,-99,-36,-23,85,2,-27,-114,13,60,-111,-61,-101,103,61,48,-44,-87,83,-28,40,109,-87,124,-77,39,22,-43,-4,53,32,-19,-71,24,-89,104,19,35,-90,-126,11,94,1,-106,-84,-117,54,-9,-23,52,-123,-97,126,27,48,-60,84,-7,-124,11,-110,84,-69,104,74,-72,-100,-67,19,19,-86,-36,-32,99,-82,47,-112,53,93,-121,-93,25,-35,53,76,109,20,-47,24,-125,41,-22,-13,-7,51,-65,81,-19,-29,-28,108,-58,80,58,67,68,63,-36,-1,0,111,-81,-112,65,73,4,-109,-54,122,-108,112,-17,61,-125,-65,20,-12,53,48,53,52,-75,82,8,-47,-97,-52,36,-37,-84,113,-30,56,98,76,-69,32,-89,-91,-26,21,57,-79,-104,-43,83,-13,-77,84,48,55,102,84,110,-126,3,-64,93,73,-73,121,-37,50,-122,-94,106,-52,-45,51,-52,26,-7,125,76,-81,-67,3,56,117,-104,-95,91,17,-79,77,42,58,32,-17,-43,99,73,69,70,-126,122,-70,-119,22,-98,4,38,-36,-28,-116,108,61,27,-32,102,21,81,74,105,53,89,-89,-89,-99,39,-122,51,-74,-51,-51,-110,-85,-60,113,-29,124,60,115,40,47,30,-51,-40,-32,-30,-82,-103,-113,-2,67,41,17,82,75,-73,-66,66,3,115,45,-80,27,-23,37,58,-3,-24,118,-14,103,126,-93,-37,-57,-120,105,83,-116,-83,107,-10,14,-77,-11,98,-101,45,74,42,-120,-90,101,11,20,46,-123,77,-66,126,-83,-72,-9,98,108,-73,44,-89,-117,58,-87,-126,59,73,89,44,-20,41,53,-22,26,-76,34,89,-100,1,-80,109,96,30,60,45,-119,-88,115,26,-45,-18,73,20,60,52,116,-2,66,-107,87,89,109,60,-40,-40,-17,-37,115,-73,28,22,-53,30,-100,-61,-50,9,-51,37,76,92,-27,59,72,20,-128,-32,113,70,-31,-70,-111,-64,113,-59,85,100,116,-117,-105,-119,-86,26,120,-23,-41,-52,-119,89,-82,16,90,-37,14,24,74,-124,-69,-13,44,-75,11,-43,-88,15,-15,-116,-85,51,125,82,65,-106,-42,-61,90,-30,59,18,-24,-115,115,111,-101,20,-44,-38,-67,-39,-106,-25,49,-84,122,-84,-15,-83,93,52,-30,-35,-57,-125,124,-60,119,98,102,-73,69,82,-60,-9,-33,-4,99,-62,80,62,68,116,-128,-1,0,-18,60,-103,-76,21,-46,79,21,20,113,10,-103,-52,42,12,-123,34,97,35,5,-66,-41,33,45,-65,110,30,60,-90,-98,47,7,41,-32,30,73,40,9,73,-35,53,49,-14,-109,-5,-21,-15,93,-75,5,-24,-114,-114,-40,-107,-25,89,43,-33,38,-84,60,-17,61,26,-56,39,-90,-86,-44,74,-77,-18,79,76,77,-25,15,-30,-15,-59,101,18,-79,-9,61,60,-10,-115,-117,-121,-25,32,109,-47,-82,-69,27,-95,83,-120,42,-57,26,89,1,61,-70,78,-60,99,-35,16,-39,-62,89,-120,-29,-83,127,-35,-15,-51,-80,4,-15,104,-49,21,-64,-121,89,-111,83,-51,39,-115,-69,49,37,5,65,-47,-52,-97,38,-20,120,-81,85,-2,108,71,5,22,111,-100,80,-45,68,73,16,-45,-42,-55,28,123,-101,-98,-120,59,111,-39,-37,-120,50,-36,-74,-104,-44,86,-43,53,-95,-89,-113,-50,-112,-10,-9,14,-46,118,0,99,58,-55,41,102,-122,-83,-78,-74,-115,-21,42,-93,13,-90,122,-87,12,-102,-12,92,-18,-118,-87,26,3,97,125,36,-11,-14,84,85,61,50,86,67,-50,67,12,-48,50,7,18,-57,36,-87,27,-117,18,7,-102,-57,-114,-40,-86,-96,-106,69,-107,32,125,28,-22,-38,-45,68,-35,36,125,-81,-59,74,-74,40,-47,-75,8,115,75,-27,-109,90,53,125,-91,-39,14,-1,0,-53,47,54,-37,111,-74,50,-4,-47,-95,84,-85,-93,-111,-78,-102,-21,50,113,65,120,73,3,123,-23,-42,-73,-33,104,-122,35,-25,1,58,-45,67,92,-15,-22,-61,83,77,-69,-61,120,100,92,87,86,65,12,-49,77,-107,-54,-119,60,-54,61,-21,85,-12,19,-40,14,-98,62,-114,-47,-127,29,103,-38,1,-3,-79,53,125,36,-80,-46,80,-47,-38,39,-85,-85,-44,-79,59,-15,8,-106,5,-103,-73,36,-19,-80,-7,-82,-87,39,-124,-71,76,20,-14,16,37,49,10,-106,58,122,-6,60,-40,-65,-94,-8,-86,25,84,117,15,53,74,-120,-22,42,-25,42,39,-87,-40,121,36,3,-34,-45,85,-55,23,36,-114,36,-38,-40,-16,-86,89,14,-89,-107,-87,-103,-113,105,60,-9,37,127,-85,-10,-117,-116,-89,61,-113,65,40,-65,-15,-75,118,85,82,29,110,98,99,99,118,-68,125,27,-37,-8,56,-26,-36,-35,-76,-24,-109,123,-111,-33,-3,-15,85,-107,85,-27,20,31,-14,53,-83,27,-51,-103,44,-74,-44,-56,-34,-8,34,-45,101,98,-73,7,73,30,115,122,49,12,17,35,-56,-60,-120,-94,-115,23,83,-54,-57,-128,0,113,36,-30,-79,106,-30,-103,102,-71,89,34,101,-46,-62,64,120,30,-50,-68,102,-43,-75,-48,-51,-108,-26,57,-60,-108,-26,-114,-98,-20,-77,-127,17,125,79,34,-1,0,41,-67,-128,60,109,-61,-81,17,-41,84,-28,127,-76,44,90,30,26,121,-3,-49,73,51,88,-128,-26,53,91,-125,-62,-6,89,65,-73,121,-60,113,-24,-126,-98,-102,13,92,-51,61,60,66,40,97,4,-36,-23,81,-33,-13,-31,-99,-56,85,81,114,79,1,-117,11,-57,75,25,-14,113,-1,0,115,-33,-113,9,-2,-117,-8,-68,-103,-121,-85,-10,-117,-118,-84,-85,50,9,-18,12,-63,108,100,49,107,106,89,0,60,-36,-53,-72,55,82,123,119,4,-116,9,-90,-121,-10,103,98,-111,86,-45,-111,45,52,-5,-111,-77,-115,-66,73,58,78,-3,-40,-26,-78,-68,-87,-21,-104,50,-93,24,-95,98,-79,-22,-31,-87,-72,40,-17,61,-104,73,-28,-98,42,-33,9,39,-114,-51,60,100,52,121,106,55,-56,-116,-115,-116,-124,121,-50,56,112,29,103,19,-42,-47,-27,-7,109,13,117,76,-46,-49,45,76,80,-22,-100,-68,-98,113,14,-6,-103,58,-4,-46,56,-100,52,-110,51,59,-71,-44,-52,-58,-27,-113,43,-47,81,63,-111,-31,44,-93,-8,-99,-61,-69,-2,-3,28,113,-31,63,-47,127,23,-109,48,-11,126,-47,121,27,-36,-75,85,20,-38,-4,-18,106,66,-70,-66,-84,8,-22,107,42,-86,16,29,90,100,-108,-80,-65,-118,-44,20,-124,-84,-10,-14,-110,127,39,112,-17,-1,0,125,28,-98,19,-3,23,-15,121,51,9,102,113,28,96,-60,11,30,2,-14,-96,-15,-51,61,57,13,88,-61,-26,-121,-65,-45,-2,-6,89,-35,-117,51,27,-110,120,-98,95,9,-2,-117,-8,-68,-103,-33,-88,-10,-15,-30,46,-99,-25,-123,66,74,11,93,-81,-37,-13,-8,-68,-58,89,32,105,47,-45,-102,-41,11,-36,59,125,56,103,118,44,-52,110,73,-30,124,79,9,-2,-117,-8,-68,-103,-33,-88,-10,-15,-31,106,41,-37,75,-81,17,-44,-61,-80,-32,9,-17,71,41,54,-77,110,-65,95,-25,108,51,26,-22,75,40,-66,-46,-126,113,-5,58,-55,84,-42,-37,109,11,-3,119,-2,-104,96,-14,24,-23,-55,-38,21,59,124,-3,-66,55,-124,-1,0,69,-4,94,76,-37,43,-90,54,-87,-98,32,-47,13,-70,108,-116,28,46,-28,113,43,107,-11,95,18,65,60,111,12,-48,-79,73,17,-41,75,35,14,32,-114,-33,-35,103,117,-13,83,-55,29,29,123,66,-108,-14,55,9,116,115,-102,-83,-36,53,13,-3,61,-121,9,69,-100,102,62,-28,-87,-110,33,50,-89,49,35,-35,110,69,-6,42,122,-63,-59,61,27,76,-29,57,-53,96,88,-22,99,-103,-11,73,48,0,14,120,31,-108,15,95,97,62,-126,125,-41,89,12,-12,-75,-58,-63,-22,105,95,67,-56,7,83,92,21,62,-101,95,97,-67,-79,-16,-20,-5,-19,-94,-3,60,124,59,62,-5,104,-65,79,31,14,-49,-66,-38,47,-45,-57,-61,-77,-17,-74,-117,-12,-15,-16,-20,-5,-19,-94,-3,60,124,59,62,-5,104,-65,79,31,14,-49,-66,-38,47,-45,-57,-61,-77,-17,-74,-117,-12,-15,28,-115,83,-99,78,-88,-63,-116,111,58,105,-109,-72,-39,1,-73,-96,-31,-22,42,30,12,-65,45,-53,-30,-20,-46,-111,40,-40,0,7,-52,0,30,-116,79,-103,-124,120,-87,85,68,20,-79,-67,-75,36,99,-74,-35,100,-110,122,-19,-86,-41,-37,17,-44,-47,84,-49,73,83,31,-101,44,46,81,-42,-5,108,71,119,-18,-13,108,-71,-21,106,-34,-126,6,-123,-29,-90,105,88,-59,27,115,9,-72,94,0,-18,126,-77,-55,-1,-60,0,39,16,1,0,2,2,1,3,3,5,1,1,1,0,0,0,0,0,1,17,33,0,49,65,81,97,113,16,32,-111,48,-127,-95,-79,-16,-63,-47,-31,-1,-38,0,8,1,1,0,1,63,33,17,-61,-76,-31,56,88,-91,43,-95,-125,76,11,105,4,-96,-123,49,-92,-97,70,12,24,48,96,-63,-125,6,126,-71,86,-126,-95,-117,0,-66,12,24,33,20,0,111,74,19,36,72,50,50,70,-35,26,76,35,-100,-119,22,-122,-52,-116,-127,90,-39,-114,8,52,54,66,-120,83,6,-5,7,94,-28,-68,-86,-86,-91,126,-125,-126,40,-46,96,39,46,54,-62,73,52,24,18,13,81,98,47,-40,64,65,1,117,81,55,-17,99,108,-94,62,102,-116,84,88,-94,-50,-32,9,-15,60,120,19,-78,74,16,118,-43,-8,-56,60,60,44,47,16,-99,29,-109,54,-42,8,-64,-44,-125,123,53,119,-14,30,-11,-31,110,10,12,-3,-121,66,-29,-27,36,-58,-70,4,-23,1,18,-65,-62,-83,-126,-3,-100,105,-96,73,-3,-50,76,119,-99,-90,-23,-126,94,24,-87,31,119,29,5,-45,-75,-3,-69,124,15,0,104,33,-3,-41,-97,113,2,51,27,104,-57,-11,-65,111,16,-78,-125,-4,5,-13,-95,-9,97,-121,56,11,64,-113,-3,-17,-49,-45,-18,46,94,73,18,48,23,-105,-116,115,-61,-124,-114,-43,7,99,-81,-80,21,125,18,-116,0,114,-71,34,29,16,34,3,57,-122,68,1,-40,12,-27,-70,44,106,-26,87,4,4,67,120,85,-76,-53,-53,-118,78,-21,-85,28,-38,5,-1,0,95,-80,-106,37,35,39,-79,-123,69,-51,104,-95,41,-37,106,23,-7,30,-112,40,93,-24,72,73,-87,18,-44,78,89,44,-100,112,84,-64,46,-76,121,-51,-112,72,14,91,-45,-124,13,54,-112,98,20,-109,-79,115,64,-85,-86,16,-63,6,81,113,-113,21,98,73,109,14,-26,82,28,36,116,-53,44,76,35,-63,55,-124,35,7,-126,-30,-29,-20,-55,-81,-47,62,86,-24,18,71,-123,4,45,34,49,-22,-30,-123,103,54,-26,-40,76,5,-114,99,19,-7,40,38,-121,-76,43,85,-91,-89,9,11,2,49,86,89,17,73,6,-53,57,56,-52,-74,-57,28,-116,-32,-86,-116,74,84,-113,88,48,73,-74,55,82,49,6,-29,-28,-79,0,18,117,2,13,26,-56,-34,36,7,-112,119,-36,-28,120,74,1,27,-24,77,-46,-18,48,59,-85,-93,-7,69,51,-1,0,101,-126,-17,-49,-64,-113,-37,-29,18,89,106,122,103,-11,39,-95,-99,65,-51,54,-119,64,104,33,70,35,26,-82,37,-91,3,-116,-70,66,80,66,32,47,52,-122,78,-78,117,109,45,-84,-79,-65,92,-61,127,64,47,46,-115,103,13,32,-106,-85,70,-91,-109,45,90,-118,67,-102,-8,-90,-35,-78,48,-40,84,-73,81,-1,0,78,-67,-29,29,-116,16,-108,-30,-70,-58,-72,-15,-112,-9,41,87,-72,45,13,35,-30,-71,41,40,56,-48,16,2,-105,77,-78,86,89,-87,108,-39,116,-126,-38,5,96,-56,-60,86,-78,2,-98,26,90,90,3,-23,102,-23,-120,41,-127,99,-54,-47,53,57,10,3,-111,20,6,68,34,2,-92,-57,92,125,65,-32,-46,-78,53,104,-127,40,-24,-88,-124,-101,-15,11,18,23,-61,108,-28,-102,75,-54,-84,101,-77,-34,63,56,123,-125,100,-43,-122,-72,-118,-98,-50,82,74,-126,-101,-118,-36,-124,105,48,-103,-109,-27,31,-88,32,-4,-99,-85,-100,48,63,44,114,5,28,-128,84,22,39,1,-29,119,76,-84,35,96,-80,-128,-11,55,-123,-28,115,-93,45,2,-127,4,16,50,-84,106,32,72,75,93,-3,104,-101,4,-49,-85,-58,-126,10,-108,86,-80,60,94,51,37,-59,-7,98,15,95,24,-90,-85,-103,-53,-105,117,-118,-107,-76,-30,64,112,-103,-47,-31,-56,109,113,-13,29,-87,-12,88,88,-63,34,-70,98,4,-53,17,69,-30,39,-109,-51,80,100,64,-96,-127,1,91,80,-30,112,89,89,-70,114,89,12,9,86,-19,-54,-30,-79,-13,-16,38,-43,-23,-99,98,80,-73,-5,120,-41,85,-11,20,117,-72,-97,73,-127,32,-28,44,-95,-104,-57,29,-92,75,34,68,-99,68,-124,-93,37,126,-92,122,75,-46,33,-76,44,-51,56,113,-104,75,101,-34,-110,52,-105,-20,-114,18,-108,-92,50,-112,52,41,-86,-15,-52,-67,2,-106,-86,-19,125,124,-26,-49,-36,-33,-101,-18,2,-119,-6,-105,-127,-90,38,87,18,-4,-28,12,106,31,-126,76,46,-19,-7,-10,-79,-118,56,-110,-109,-27,14,-8,-102,-65,104,120,-123,80,32,79,75,75,-9,-40,91,-50,-63,-27,-6,31,118,-96,-61,-80,-13,-14,-82,-43,-21,-18,28,16,109,78,16,-95,-49,65,51,-28,-102,125,-102,-49,19,28,83,-54,66,126,-56,-44,-51,59,15,63,42,-19,94,-66,-15,-93,89,-112,-105,-50,14,71,-6,-15,99,33,-77,-54,-24,-102,-44,-94,19,-9,-58,74,66,42,-35,1,-107,-20,99,82,46,81,-55,-79,118,69,-19,-57,-37,95,-120,81,42,14,-83,13,-13,-88,-6,3,66,-14,110,25,-32,-119,51,22,113,24,-18,106,-78,-56,82,-64,-120,-113,-46,59,-119,20,44,93,117,3,-124,-62,102,13,-66,40,26,25,3,-94,-103,-84,-78,116,-23,-87,54,-104,121,66,119,-106,-109,-118,33,-24,6,-64,116,39,68,125,30,-99,58,116,-23,-45,-89,65,-74,-13,-92,50,-35,45,48,55,73,-68,26,-4,20,2,-127,-17,2,-107,64,74,25,-71,-41,-17,-12,-93,101,-109,-56,0,-29,-92,-42,-83,-54,-108,36,-92,-16,-71,115,-29,-23,57,-74,-14,77,-97,-88,-61,-26,125,63,-1,-38,0,8,1,1,0,0,0,16,-64,0,0,-32,56,0,96,-127,8,0,0,-43,-116,-98,4,-11,-113,-96,93,-64,-124,8,66,6,0,-96,36,2,0,15,-7,0,47,-1,-60,0,34,16,1,1,0,3,0,2,2,2,3,1,0,0,0,0,0,0,1,17,0,33,49,32,65,16,81,48,97,113,-127,-111,-79,-1,-38,0,8,1,1,0,1,63,16,72,-72,-99,-15,-67,120,-49,92,96,-3,107,-9,-47,49,4,33,24,9,-8,35,-57,-113,30,60,120,-12,-57,-39,-120,-5,-113,-101,76,-125,17,116,56,47,-120,47,27,-44,-32,26,-126,-27,88,-68,64,-116,121,-78,94,-31,6,3,99,-86,-100,-105,113,24,-124,-111,-110,9,125,-54,67,107,-68,-123,127,8,117,-49,-60,-22,34,63,120,43,35,11,100,86,-127,-56,-32,27,69,107,13,124,-84,93,46,54,0,11,-27,58,-74,-83,121,10,-128,-84,5,97,-96,92,32,64,66,45,8,34,-37,32,29,85,-63,34,48,-125,32,72,-96,69,-35,109,-36,-128,112,-76,59,112,-91,53,14,53,-87,120,-116,80,-127,86,84,-78,-78,41,20,70,-32,-25,-50,-26,-23,48,120,21,40,-72,-82,-128,19,-96,39,-18,-83,-61,9,-2,-40,5,-127,94,14,36,69,-1,0,-88,-40,-120,32,68,65,17,7,1,-6,72,45,-67,0,13,-125,78,115,-53,54,104,88,63,64,-18,-46,-124,-82,-62,-117,87,0,89,-47,-80,122,-75,-38,-86,-86,85,42,-86,-81,-106,-42,-51,46,112,88,69,40,87,-85,9,-31,-97,48,8,110,-75,7,65,-113,98,112,11,15,122,-117,-45,-43,87,106,85,82,-87,85,85,127,29,-25,-86,97,58,10,-124,5,83,106,7,-68,-25,57,18,45,80,16,-64,67,69,-76,-65,35,40,-41,-126,12,42,16,0,-86,-103,59,2,81,80,87,56,49,-73,-59,-59,74,122,-106,-89,20,-44,73,-80,-88,-43,-128,-36,-65,75,1,-95,-47,0,1,-112,104,49,18,-120,8,32,34,35,-16,-89,-60,-57,-72,85,24,-106,-117,106,0,-51,-56,-114,85,12,41,15,-22,-128,37,73,42,103,70,-95,-24,85,121,-39,58,-119,-58,24,48,-54,-127,-125,-25,34,10,60,30,-127,-23,64,-99,96,28,122,25,71,57,-3,46,3,19,73,-100,-124,64,-100,36,-104,-75,-121,102,60,7,-117,26,113,-126,1,122,-88,16,-5,26,112,114,70,-111,-6,-119,90,79,10,23,-29,55,-75,36,97,24,-60,-121,82,14,-125,105,-124,-62,108,92,-13,-99,-127,32,80,16,-120,-76,-58,80,99,90,3,100,44,-86,-23,125,89,-109,-106,45,-63,4,24,58,55,48,9,36,97,25,3,11,16,-71,-6,-116,-92,74,67,-96,67,-11,90,80,89,20,45,-110,-48,-45,-60,-100,44,-13,-51,95,43,0,-127,75,69,-100,87,-17,-54,91,-86,91,61,107,-123,-8,-40,-126,-44,-91,-37,78,58,-10,-11,97,23,61,23,-117,125,-38,-41,-33,-35,-8,12,48,-21,-70,-110,-9,19,-128,-107,53,55,-26,-60,-86,-64,-120,68,76,35,42,75,-78,-44,72,107,-51,28,91,-57,102,50,37,53,-111,2,-80,-111,-9,-128,32,6,91,123,25,35,17,-24,-12,-84,104,116,3,-63,-95,-95,-40,-117,-115,80,-92,-92,50,8,-89,-86,-16,-116,-63,-19,-95,50,-22,-38,-120,48,-128,-128,2,100,40,-58,-22,59,88,23,-96,-110,16,24,-74,-117,-2,-39,5,-58,14,-23,-42,-112,71,7,-44,-107,-10,25,-126,-122,44,78,75,78,116,-116,4,-19,124,52,63,31,67,-60,-49,4,-45,-37,38,31,94,8,115,48,89,69,91,-122,-96,-72,-21,102,-54,41,48,-11,92,10,23,-121,-54,-96,29,-111,-107,-128,-63,40,54,7,-94,-47,-74,126,-49,103,-72,53,82,64,67,17,127,-29,87,115,43,90,-33,-126,42,99,100,120,-46,1,-25,-21,18,-49,-18,127,39,16,-3,-70,-61,-22,-37,-114,-58,104,-31,9,37,-80,-63,-30,95,97,5,-43,57,-92,-108,18,-29,126,-11,69,44,-101,52,99,-42,41,92,-95,0,42,88,1,-16,110,93,1,-83,125,60,97,34,-16,-23,102,70,42,17,73,65,-24,64,-123,109,86,110,63,-102,-82,0,-101,-76,-100,-103,-45,-16,49,67,-118,-10,-126,-95,-5,112,-100,-105,-44,-96,58,30,-32,-104,-47,3,-100,-59,-121,-79,38,-34,-14,-90,66,97,43,17,-73,108,30,12,48,96,95,-79,-104,-17,65,21,70,34,-64,112,112,-104,-110,74,-122,-128,10,-82,-128,-61,-92,-87,77,-99,-102,-104,-79,97,-80,-88,-73,-64,-54,-61,38,-92,6,-95,-78,-125,-125,103,33,-19,34,43,-68,70,106,25,76,35,6,26,32,-53,103,53,-100,80,16,126,-18,-5,122,-37,104,-120,-53,-79,95,42,5,108,-98,-96,-36,-112,56,66,124,-75,21,49,80,-86,-83,85,-8,88,101,-98,-7,-4,2,23,-12,31,-48,-23,-14,74,-58,-94,107,-117,-12,-77,-48,94,84,-21,-127,68,-61,36,2,-96,0,89,98,61,-8,-125,-75,12,75,-62,125,82,-99,17,87,112,-75,-16,-105,115,-81,69,82,-67,38,86,-127,-86,2,-32,-45,-53,-124,-9,1,42,11,-91,-115,125,68,12,7,-64,-29,89,42,27,82,-86,-69,87,-58,70,-1,0,-86,-53,71,-8,0,52,64,-65,114,43,67,-31,40,42,-61,11,-40,-128,-128,-71,6,-119,-72,108,82,-84,-66,7,26,-55,80,-38,-107,85,-38,-66,114,-97,-78,-80,4,83,108,105,26,-46,32,-120,4,-34,-80,-100,-109,12,64,2,1,-20,65,-61,19,51,0,21,22,-46,-48,42,-24,23,28,-25,73,81,-111,112,3,-96,52,94,-31,119,121,-36,59,64,8,54,88,82,11,15,57,-38,-24,54,-38,-114,-121,71,-77,17,36,-51,-23,33,-110,-126,-64,17,17,4,-4,68,-108,4,54,104,90,19,22,69,11,62,-103,44,-43,-29,52,116,6,-46,34,-78,47,97,-64,-104,10,91,-67,-52,-93,60,-5,14,-97,106,55,-87,64,-2,31,62,124,-7,-13,-25,-49,-103,78,-76,9,-45,48,69,3,21,80,127,38,-63,-42,-103,65,-92,41,0,19,0,36,-24,-112,-87,-120,-112,-58,-121,-32,61,-36,73,35,-60,-94,116,-90,37,47,99,-2,126,37,-59,73,72,20,54,44,-127,122,116,-49,-1,-39,};
	public static final byte [] unoR = {-1,-40,-1,-32,0,16,74,70,73,70,0,1,1,2,0,37,0,37,0,0,-1,-37,0,67,0,80,55,60,70,60,50,80,70,65,70,90,85,80,95,120,-56,-126,120,110,110,120,-11,-81,-71,-111,-56,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-62,0,11,8,0,-102,0,100,1,1,17,0,-1,-60,0,24,0,0,3,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,2,3,4,1,-1,-38,0,8,1,1,0,0,0,1,-19,64,0,5,-51,-81,47,45,0,0,-45,43,-29,-48,103,30,-127,-58,75,-27,-47,5,-78,-49,-125,-23,-123,-14,82,118,-110,-128,106,-107,-15,-105,-110,-128,26,18,-8,-24,77,-84,69,77,9,124,-106,-128,106,124,-45,52,-50,-16,69,47,84,-108,-5,-82,23,-108,10,-35,39,62,62,-104,104,-49,38,-81,105,-103,11,-76,-12,100,86,-41,-52,-56,59,58,95,24,-38,-71,-99,59,120,93,47,-116,0,44,-119,-95,52,99,-32,5,-105,-123,83,70,121,7,108,-119,117,100,-68,69,110,-55,94,-47,-86,95,39,89,84,29,-29,-90,118,56,0,7,91,42,-21,-24,0,6,105,-1,0,-1,-60,0,31,16,0,2,3,1,1,1,1,1,1,1,0,0,0,0,0,0,1,2,0,17,49,16,18,32,33,50,48,34,-1,-38,0,8,1,1,0,1,5,2,85,-1,0,50,46,31,-61,9,-72,33,95,-49,-75,54,31,78,69,20,9,-24,91,-108,-94,122,19,-48,48,-84,76,125,56,54,-86,49,-66,5,-88,91,-31,77,113,-12,-28,-9,-64,60,-62,111,-23,49,-12,-25,20,84,38,-2,-45,31,78,69,23,24,-16,84,-16,39,-127,15,-31,-30,99,-23,-56,127,-28,117,114,55,-11,-60,-57,-45,-119,9,-77,-59,88,77,64,-41,27,-6,-32,-57,-45,-121,-15,120,-102,77,74,-72,-62,-70,-69,31,99,-20,26,-62,-94,-114,49,-66,-113,-59,76,125,-121,96,-34,49,-66,-88,-78,-26,38,62,-100,-32,-37,18,-60,97,-33,-27,98,99,-23,-49,-75,16,-101,-30,99,-17,-38,-84,99,112,11,-123,106,38,62,-57,31,33,106,51,95,20,84,99,97,49,-12,-32,48,-92,-14,103,-119,96,66,111,-117,-79,-115,-108,-57,-34,92,-12,101,-4,41,-88,-52,15,19,31,65,-72,69,-49,34,121,19,-56,-98,68,-14,39,-111,60,-119,-28,79,35,-116,108,-63,-97,-30,-5,63,-1,-60,0,29,16,0,2,2,3,0,3,0,0,0,0,0,0,0,0,0,0,33,48,0,17,1,32,64,16,49,80,-1,-38,0,8,1,1,0,6,63,2,-20,-67,-67,67,2,11,47,44,-68,-68,-86,-69,-116,10,16,-21,109,-82,-117,-53,47,44,40,-84,-63,-32,-22,126,95,-1,-60,0,32,16,0,2,2,2,3,1,1,1,1,0,0,0,0,0,0,0,0,1,17,49,16,33,32,65,97,81,48,-111,113,-1,-38,0,8,1,1,0,1,63,33,76,75,40,-110,73,36,-110,73,39,9,-80,-102,7,-92,61,-115,-98,-28,74,-108,-78,73,36,-109,102,-60,-38,-17,33,-33,-124,72,29,27,-89,-106,14,-55,21,10,8,41,-87,-50,-17,40,33,-77,122,37,121,-124,108,30,-12,-76,-72,74,-16,81,-42,59,-60,-31,-55,47,-123,-79,9,-40,61,-71,52,-29,-33,-104,0,-10,-4,126,-4,78,-35,19,-72,84,-79,51,-56,-105,71,31,-68,74,88,-38,87,-63,90,-37,23,101,49,-17,19,115,-13,-124,69,-74,33,37,-118,23,101,33,22,59,-118,127,114,-110,80,-126,-73,-8,21,16,-36,-71,120,89,41,-52,-25,106,98,-95,-69,6,-40,110,22,-55,121,89,-123,-39,-34,91,21,-31,-72,68,-81,56,7,67,63,102,104,61,-113,65,9,-23,-25,-5,62,31,127,-31,18,9,-100,61,-48,-12,-7,127,-128,74,-123,71,88,114,75,-31,-16,57,-23,-16,-79,27,26,10,-79,14,93,-16,-113,-72,92,7,-48,121,-117,-24,116,-10,-58,91,9,35,119,-62,30,-44,-88,26,-121,2,101,76,-9,27,59,124,36,75,16,-31,35,30,48,-101,35,-52,-13,60,-49,51,-52,-13,60,-49,49,35,-94,-80,9,-63,71,-28,-49,31,-1,-38,0,8,1,1,0,0,0,16,0,1,7,-109,-34,33,-16,-2,-73,-3,127,-25,-81,125,75,-113,73,-14,95,118,113,-65,-35,-6,-25,47,39,-34,61,-48,0,127,-1,0,-1,-60,0,33,16,1,0,2,1,5,0,3,1,1,0,0,0,0,0,0,0,1,0,17,49,16,33,65,81,113,32,97,-95,48,-127,-1,-38,0,8,1,1,0,1,63,16,32,27,122,-101,14,-119,78,-55,78,-55,78,-55,78,-55,78,-55,78,-55,78,-55,78,-55,99,-55,55,97,-65,113,-47,113,16,-117,-126,41,-35,-82,37,68,84,122,-120,6,-63,-36,-73,108,-73,108,-73,108,-73,108,-35,-36,81,-101,-118,88,-82,61,-18,76,-63,67,-55,-8,52,-27,75,6,-47,-60,26,-17,-8,59,101,77,-10,-57,104,85,3,108,101,-94,-65,-87,-105,-39,-117,-55,-8,33,18,113,114,-30,-60,96,-22,47,80,-63,1,90,51,11,31,-4,-126,83,5,-68,-22,-123,-37,36,32,-66,91,-52,94,79,-63,40,7,17,114,-111,82,-99,-125,60,105,-111,98,113,-63,-14,-95,-6,-104,-68,-97,-125,83,127,-14,-106,39,28,31,60,-34,-52,94,79,-63,-90,-29,-125,90,-127,69,37,-13,62,-26,125,-116,53,24,53,-51,-20,-59,-28,-4,17,0,50,-62,20,-35,-16,11,127,62,50,-85,-9,49,121,63,4,53,60,67,43,-82,-56,-9,-32,-106,2,58,-114,-50,-80,45,3,-103,-11,65,49,121,55,121,77,-88,-39,-44,91,-123,-47,44,12,85,-57,-57,72,-62,20,-57,-26,52,-90,-24,-34,27,-89,13,76,94,68,18,-103,117,88,13,63,84,81,-68,-20,34,41,-43,-32,96,50,-88,34,125,38,53,-78,50,-30,43,71,-106,98,-14,98,88,-82,-9,-45,-12,104,12,-72,34,-11,14,53,82,56,51,45,76,4,-51,-20,-59,-28,116,-57,83,58,58,103,-71,-12,98,-27,34,-91,-120,-111,-48,21,-93,51,104,74,-83,-71,-103,-67,-104,-68,-97,-125,-8,11,109,6,35,-70,-29,-115,51,123,49,121,5,-93,-72,109,58,126,69,88,4,116,73,76,-59,-11,44,65,51,123,49,121,20,11,113,54,95,-64,5,80,91,12,-69,28,68,-32,64,43,70,96,75,13,52,-115,-31,-103,-67,-104,-68,-97,-118,59,-113,-36,4,-69,-66,-76,-108,-106,36,75,70,15,-38,-1,0,52,125,-84,-101,-112,-39,120,86,9,-69,85,28,76,-34,-52,94,75,-98,-60,119,92,71,-19,13,6,76,126,6,-3,18,-125,-72,-24,-5,-100,-52,94,66,-65,107,-28,-100,-111,-4,64,0,0,19,-77,116,80,110,-127,44,83,28,68,86,41,29,-77,-41,-14,-79,45,-83,63,-1,-39,};
	public static final byte [] unoS = {-1,-40,-1,-32,0,16,74,70,73,70,0,1,1,2,0,37,0,37,0,0,-1,-37,0,67,0,80,55,60,70,60,50,80,70,65,70,90,85,80,95,120,-56,-126,120,110,110,120,-11,-81,-71,-111,-56,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-62,0,11,8,0,-101,0,100,1,1,17,0,-1,-60,0,24,0,0,3,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,2,3,4,1,-1,-38,0,8,1,1,0,0,0,1,-19,64,0,5,-51,-81,40,-45,0,13,50,-66,103,100,-109,-48,57,-43,-68,25,120,-21,62,15,-94,55,-56,-27,36,-128,26,103,124,101,-111,0,11,-83,-15,-48,-104,0,93,111,-110,-48,-19,-33,-103,-44,-45,59,-63,57,-94,107,-91,114,-102,-29,121,65,-19,29,16,-66,71,-47,29,25,-28,-6,12,-51,92,-41,23,70,69,-18,-109,-99,-50,83,-85,124,97,-42,20,-68,44,-73,-58,0,21,84,-70,-33,39,0,42,79,-105,93,25,-90,29,-95,-42,56,-73,-113,57,-41,-110,-24,80,91,-28,110,-15,2,-35,-99,82,-22,-96,0,118,-103,-105,88,0,1,-98,95,-1,-60,0,33,16,0,2,2,2,2,3,1,1,1,0,0,0,0,0,0,0,0,1,2,49,17,18,16,33,3,32,48,34,50,65,-1,-38,0,8,1,1,0,1,5,2,-116,126,109,100,107,12,111,34,77,-102,-65,-124,94,84,-19,-44,86,91,101,19,-31,71,38,18,54,-119,-104,-114,39,-116,-99,-70,-123,-1,0,34,89,114,-19,-88,-32,114,-12,-117,-64,-119,-37,-93,118,108,-40,-106,-93,121,-10,-123,78,-35,113,20,55,-97,127,25,59,116,69,100,-109,-8,120,-55,-37,-95,-2,99,-62,-127,-124,56,-95,-84,115,10,-99,-70,-126,27,-53,34,-80,-91,33,101,-117,-94,85,-62,-87,-37,-89,-44,72,-36,-23,44,-99,69,57,100,116,69,101,-1,0,-77,-78,118,71,-6,107,39,81,27,-55,21,-110,111,-94,61,40,19,-79,-33,17,-106,71,28,-102,33,-76,-122,-14,37,-105,54,120,-55,-37,-81,77,-103,-77,-27,126,81,-29,39,110,-67,-30,-119,60,-15,-29,39,111,-34,49,-55,39,-49,-116,-99,-110,93,-6,40,-110,-112,-106,18,121,36,-113,25,59,116,-98,71,3,86,104,117,17,-53,60,53,-111,-68,21,15,25,59,116,108,-51,-39,-77,-12,82,63,35,121,32,78,-45,-56,-30,-103,-94,52,70,-120,-47,26,35,68,104,-115,17,-94,-30,79,-76,-2,114,125,-97,-1,-60,0,29,16,0,1,4,3,1,1,0,0,0,0,0,0,0,0,0,0,17,0,1,33,48,16,32,64,49,96,-1,-38,0,8,1,1,0,6,63,2,-20,12,-89,73,94,98,54,47,-119,81,89,123,11,-4,65,79,96,-24,54,-122,-82,84,34,-5,7,-35,-112,107,39,-65,-1,-60,0,34,16,0,2,2,3,0,2,3,0,3,0,0,0,0,0,0,0,0,1,16,17,33,49,65,32,97,48,81,113,-127,-95,-79,-1,-38,0,8,1,1,0,1,63,33,74,86,-51,22,89,101,-106,89,101,-62,118,48,3,-62,30,-48,-115,104,-55,101,-106,89,101,-65,-71,-113,124,3,-20,19,-82,89,98,83,-75,-40,96,-17,64,-69,-126,26,-77,-44,123,-58,-48,105,101,-74,56,6,-46,41,44,-33,-116,60,45,122,17,93,-82,-57,-66,71,-128,33,58,15,111,38,-72,-5,-27,41,5,-17,62,-93,-33,23,-77,-94,-9,75,75,-32,-22,61,-30,-53,52,59,9,91,-92,33,108,-12,33,-95,-17,-29,-5,-52,-106,-7,48,-101,-122,-68,45,8,-52,9,77,-117,115,90,72,-9,26,127,113,-102,-58,-10,-128,-58,-12,127,-116,-118,118,-53,-22,119,-45,-22,53,21,70,-48,61,-117,-41,-62,-118,66,94,-58,110,91,91,74,19,-40,-122,-74,32,66,49,-83,-104,-125,-128,-22,61,-34,62,-29,-35,41,-107,-20,110,-39,-44,123,-2,14,-30,-33,-88,-22,52,-76,-4,-10,30,-113,-52,42,58,-98,-85,115,-57,-80,-63,90,10,1,-113,-123,-127,109,90,58,-113,112,-76,6,112,-11,-119,-69,-125,-7,-104,-32,-74,126,33,88,63,-80,-22,60,-104,-48,-111,-45,0,-39,-33,12,116,45,50,-117,-30,-30,-29,-95,127,16,85,85,32,-47,113,98,120,-8,-35,101,120,-113,-1,-38,0,8,1,1,0,0,0,16,-128,17,1,65,-54,101,-14,63,-69,-3,127,-89,93,108,123,-114,-52,-42,66,-105,96,-65,-10,-6,-81,105,98,-120,-66,95,-16,127,-1,0,-1,-60,0,34,16,1,0,2,1,4,3,1,1,1,1,0,0,0,0,0,0,1,0,17,49,16,33,65,81,97,113,-95,32,48,-111,-127,-1,-38,0,8,1,1,0,1,63,16,0,54,-77,97,-47,41,-39,41,-39,41,-39,41,-39,41,-39,41,-39,41,-39,41,-39,44,121,33,123,55,-18,50,46,34,-79,120,-117,27,-93,-126,98,-18,-69,101,-62,-17,-45,47,-77,45,-37,45,-37,45,-37,45,-37,45,-37,2,108,87,23,123,36,-59,-22,124,18,-92,113,-52,-31,80,-37,105,92,89,-63,-44,49,-63,-93,22,81,-37,1,93,-114,-128,-38,53,-103,-52,117,-93,-117,-44,-8,33,9,114,75,90,91,-119,-61,-52,-17,54,-19,-43,-79,62,2,-114,40,-92,85,-53,-86,30,-2,68,18,-109,23,-87,-16,65,70,-52,-62,-118,65,-9,42,112,-16,105,-111,98,113,-63,-6,-54,113,49,122,-97,6,-65,-28,-24,-50,-34,-36,126,-45,67,-119,-117,-44,-8,52,-36,112,127,18,1,-78,-104,-67,79,-126,4,3,44,116,28,-76,98,11,89,-68,59,-15,-94,3,-79,79,-119,64,113,-61,-88,75,60,-77,23,-87,-16,64,86,40,100,120,-30,5,-75,44,12,-14,-60,21,-88,-33,71,-36,-40,91,121,96,-77,-55,-71,-90,101,122,98,-11,55,122,-94,-88,103,64,4,-60,-108,57,96,42,54,-18,5,13,-126,86,-51,-83,50,-24,-88,56,55,-107,-122,35,23,-88,-105,45,-22,-48,-22,-56,32,45,3,4,-104,-24,-106,39,28,17,5,27,38,-44,-53,-89,112,-79,21,-114,-26,47,81,-40,-71,112,-13,-96,-46,39,16,16,-75,-46,94,-122,9,-67,-39,75,42,-8,9,96,37,23,14,96,1,-120,-50,-114,47,83,103,-85,-16,40,-39,-77,1,42,21,42,21,91,91,-127,109,64,124,-40,-120,-71,116,113,122,-97,7,-16,10,113,-104,-117,-32,99,87,23,-87,66,-20,-119,74,117,-6,-71,-124,65,78,4,-73,78,-82,47,81,106,59,3,119,-32,47,18,-13,25,-44,23,10,63,-50,100,-37,40,121,72,-105,57,-42,-114,47,83,-30,-97,-28,-74,5,-67,-26,-127,-68,4,-38,-78,8,-17,-79,-44,-63,-18,49,3,67,48,-2,-103,11,107,119,-19,-93,-117,-44,20,14,72,-118,-89,100,-104,-107,48,-81,-2,-52,-54,-4,17,-9,76,114,-40,-8,40,48,71,26,-26,98,-11,8,68,98,-45,126,-25,-111,-98,70,121,25,-28,103,-111,-98,70,121,25,-28,96,-101,-35,-128,10,54,8,77,88,54,-124,-83,80,-36,63,-111,-53,-95,-1,-39,};
	public final byte [] unoT = {-1,-40,-1,-32,0,16,74,70,73,70,0,1,1,2,0,37,0,37,0,0,-1,-37,0,67,0,80,55,60,70,60,50,80,70,65,70,90,85,80,95,120,-56,-126,120,110,110,120,-11,-81,-71,-111,-56,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-62,0,11,8,0,-101,0,100,1,1,17,0,-1,-60,0,24,0,0,3,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,2,3,1,4,-1,-38,0,8,1,1,0,0,0,1,-38,-32,0,1,14,-114,84,-94,96,104,94,-71,-51,-82,-109,122,1,-105,-50,93,-76,40,75,6,-23,108,-29,54,-45,64,14,-118,-25,25,100,64,2,-67,25,-58,-19,38,113,20,-81,70,114,90,57,94,-125,-102,101,-19,-112,-55,-69,-75,57,-26,117,62,79,-100,-24,-88,-111,-101,116,-79,8,-99,21,37,36,-74,-40,-27,67,-94,-89,30,109,-105,-93,57,48,-67,115,-105,47,39,-24,-50,48,0,-90,-54,-67,7,30,0,59,-60,-81,65,-49,32,43,-71,-119,78,-116,-114,35,58,78,-53,68,-24,-50,70,101,92,45,-109,-83,-41,16,0,13,-94,67,-84,0,0,-97,55,-1,-60,0,32,16,0,2,2,3,0,3,1,1,1,0,0,0,0,0,0,0,0,1,2,17,16,18,49,32,33,50,48,34,65,-1,-38,0,8,1,1,0,1,5,2,-116,113,104,-76,90,45,22,-117,69,-94,-47,120,112,-60,-99,-29,87,-8,65,-34,31,18,-79,-86,21,-73,46,-119,89,73,27,35,100,106,-103,14,-113,-119,-47,118,-38,-12,-43,10,35,-105,-126,116,47,120,124,-54,84,55,126,80,-61,-26,34,-119,59,-13,-121,71,-62,42,-36,-98,18,-77,70,104,-57,-21,48,-24,-8,124,-57,16,-18,37,-11,-120,97,-14,11,-36,-99,-79,70,-59,22,-113,-24,94,-55,125,98,28,31,62,99,-120,115,17,-28,-66,-123,-33,-9,19,-52,57,-123,37,82,-10,-56,17,-6,-60,-69,-120,115,-58,94,-108,59,-29,7,-22,-47,107,49,84,-97,-78,29,31,60,-30,-84,-109,-60,59,-8,69,89,39,89,-121,113,53,-30,-94,57,26,-114,53,-120,116,124,-117,28,10,102,-116,-91,17,-54,-15,20,55,111,-42,-80,-18,118,102,-20,-39,-8,90,106,77,81,12,39,105,-58,-51,13,13,13,13,13,13,13,13,48,-28,-124,-1,0,57,-16,-1,-60,0,29,16,0,1,4,2,3,0,0,0,0,0,0,0,0,0,0,0,17,1,32,33,48,0,16,49,64,80,-1,-38,0,8,1,1,0,6,63,2,-16,103,92,100,56,38,-89,33,-91,-59,108,54,-113,60,88,109,22,30,-56,75,2,103,46,10,-14,-74,-50,71,123,-1,-60,0,33,16,0,2,2,2,3,1,1,0,3,0,0,0,0,0,0,0,0,1,16,17,33,49,32,65,97,113,48,81,-111,-95,-1,-38,0,8,1,1,0,1,63,33,85,91,-113,67,-40,-10,61,-113,99,-40,-10,61,-124,-113,-79,-92,-42,70,94,33,-83,-28,98,26,-89,-98,122,78,55,15,98,-3,-12,61,-82,-121,79,24,108,58,-67,34,-19,-122,-115,-108,-89,78,55,-106,6,45,-128,-79,69,89,29,124,12,108,20,-125,124,-28,82,3,-2,57,62,42,55,-49,120,-75,-25,-29,-17,-112,-21,69,-54,83,73,106,-29,-17,-113,-20,124,-67,-46,-69,113,-68,-78,-33,-60,-61,-75,52,-52,-92,-20,106,89,-70,82,-93,-72,-1,0,94,59,-44,59,-95,45,17,112,60,-113,-102,-29,-67,10,81,-79,-78,-88,78,-61,91,-71,107,126,59,-48,-9,9,91,-95,-79,-89,122,30,-27,11,38,123,14,-83,-50,96,53,-82,125,-33,-123,-81,11,-16,-75,-57,120,111,-107,-81,10,92,-114,-121,107,-117,-34,94,17,80,36,98,-82,-8,123,-124,-76,12,-24,-14,16,118,-74,63,76,40,74,65,-70,-48,-41,-54,118,-83,13,83,-95,35,-72,27,59,-32,-86,69,2,23,-118,23,55,25,-127,67,-20,-5,62,-49,-77,-20,-5,62,-49,-79,47,108,73,45,9,58,24,-98,63,54,107,8,-1,-38,0,8,1,1,0,0,0,16,0,19,56,-55,106,81,-106,126,-73,-3,125,-27,-19,30,105,-22,-49,27,121,-5,-119,-65,-99,-1,0,-17,-117,127,-98,-40,112,6,127,-1,0,-1,-60,0,34,16,1,0,2,2,1,5,1,1,1,1,0,0,0,0,0,0,1,0,17,16,49,33,65,81,97,113,-95,32,48,-111,-127,-1,-38,0,8,1,1,0,1,63,16,17,59,89,96,115,-60,-15,103,-125,60,25,-32,-49,6,120,51,-63,-98,12,74,-128,-54,-48,-78,5,-42,-56,-16,122,-118,121,65,-94,110,13,106,-1,0,-111,24,20,-97,-128,93,17,19,100,20,108,106,47,116,117,-57,-59,56,17,-57,88,-107,-114,15,23,12,-66,4,116,-12,97,-2,14,59,-64,66,-83,-115,-103,-111,87,16,24,-122,41,12,124,17,1,24,-58,45,11,-88,-43,16,117,-113,97,21,-19,12,113,-121,105,-76,104,-17,21,118,-34,72,42,112,14,-45,31,6,111,-71,-99,87,-12,24,-17,110,-49,-43,-23,-12,-57,-63,-102,122,97,-88,-98,54,-65,123,-67,99,-32,-59,55,99,112,-102,94,13,-31,100,121,9,-28,34,-86,-39,-99,-34,-79,-16,64,86,-115,-57,-89,37,-74,-15,-69,-41,-22,116,-52,124,19,-104,106,45,107,70,2,90,84,66,-64,102,-46,26,-23,43,105,95,-119,67,-25,31,20,-29,79,57,110,-9,-115,-98,-90,-81,121,-98,72,96,0,29,24,20,-87,104,61,51,-69,-34,54,122,-100,72,55,5,52,98,-69,14,13,126,58,-80,61,-13,-69,-34,54,122,-101,61,-31,-116,109,-128,45,-53,-71,-69,-42,29,55,-60,86,-97,57,104,0,-36,-16,97,-101,71,-120,-14,-82,1,56,-5,92,85,83,119,-84,124,95,-63,-83,78,27,-105,122,76,110,-11,-124,-79,33,-91,-25,-12,-73,60,13,-80,-76,103,92,-18,-11,-123,-94,-39,-37,103,-12,40,7,72,117,-127,10,109,-124,15,43,-108,-51,-34,-79,-15,79,-15,123,2,-33,51,-92,-26,-85,-60,-68,-107,14,67,-72,-30,48,63,-55,-46,-111,9,21,-68,-8,51,101,118,-59,-55,-36,-116,-53,-124,-123,-47,-62,120,-119,-37,-98,-94,-85,107,121,32,-11,81,-36,-107,117,-106,121,56,-19,26,-12,-30,-80,5,-43,-42,108,-8,123,-1,0,31,-1,0,-14,55,97,13,-95,68,-38,-113,-88,74,-43,14,67,-7,90,-43,94,63,-1,-39,};
}





class Sounds {
	/* 
	 * these are the three sounds which are stored in byte arrays
	 * the audio is in .mid format so that it takes the minimum amount space
	 * this means that it is storing only the notes to play and the instruments
	 * 
	 * all the sounds where generated using 
	 * winner = pizza time			- https://onlinesequencer.net/1496934#t36 - modified
	 * loser  = Beethoven 5th 		- https://onlinesequencer.net/275900
	 * click  = drumkit castanets 	- https://onlinesequencer.net/
	 */
	public static final byte [] winner = {77,84,104,100,0,0,0,6,0,1,0,13,1,-128,77,84,114,107,0,0,0,19,0,-1,88,4,4,2,24,8,0,-1,81,3,5,-24,24,0,-1,47,0,77,84,114,107,0,0,0,28,0,-1,3,8,68,114,117,109,32,75,105,116,0,-55,0,-87,122,-103,55,50,68,-119,55,0,0,-1,47,0,77,84,114,107,0,0,8,1,0,-1,3,12,83,109,111,111,116,104,32,83,121,110,116,104,0,-64,80,-126,1,-112,68,50,0,-112,59,50,14,-112,64,50,54,-128,59,0,0,-128,68,0,14,-128,64,0,22,-112,66,50,0,-112,57,50,12,-112,62,50,2,-112,78,50,0,-112,74,50,55,-128,66,0,0,-128,57,0,12,-128,62,0,2,-128,74,0,0,-128,78,0,-127,75,-112,66,50,0,-112,57,50,12,-112,62,50,57,-128,66,0,0,-128,57,0,12,-128,62,0,22,-112,59,50,0,-112,68,50,12,-112,80,50,2,-112,64,50,3,-112,76,50,52,-128,68,0,0,-128,59,0,13,-128,64,0,-127,68,-112,68,50,0,-112,59,50,14,-112,64,50,54,-128,68,0,0,-128,59,0,14,-128,64,0,8,-128,80,0,5,-128,76,0,10,-112,66,50,0,-112,57,50,12,-112,62,50,0,-112,78,50,4,-112,74,50,53,-128,57,0,0,-128,66,0,12,-128,78,0,0,-128,62,0,4,-128,74,0,-127,73,-112,66,50,0,-112,57,50,12,-112,62,50,44,-112,76,50,8,-112,80,50,5,-128,66,0,0,-128,57,0,12,-128,62,0,22,-112,69,50,0,-112,59,50,12,-112,81,50,2,-112,78,50,0,-112,66,50,9,-128,76,0,8,-128,80,0,38,-128,69,0,0,-128,59,0,12,-128,81,0,2,-128,66,0,0,-128,78,0,-127,68,-112,68,50,0,-112,59,50,12,-112,80,50,2,-112,64,50,2,-112,76,50,52,-128,59,0,0,-128,68,0,12,-128,80,0,2,-128,64,0,2,-128,76,0,20,-112,62,50,12,-112,57,50,0,-112,66,50,0,-112,78,50,4,-112,74,50,53,-128,62,0,12,-128,57,0,0,-128,78,0,0,-128,66,0,4,-128,74,0,-127,71,-112,66,50,12,-112,81,50,2,-112,57,50,0,-112,69,50,4,-112,78,50,51,-128,66,0,12,-128,81,0,2,-128,69,0,0,-128,57,0,4,-128,78,0,16,-112,64,50,12,-112,68,50,2,-112,80,50,2,-112,59,50,3,-112,76,50,50,-128,64,0,12,-128,68,0,2,-128,80,0,2,-128,59,0,3,-128,76,0,60,-112,71,50,0,-112,68,50,68,-128,71,0,0,-128,68,0,58,-112,73,50,0,-112,69,50,22,-112,59,50,46,-128,69,0,0,-128,73,0,22,-128,59,0,22,-112,59,50,6,-112,71,50,12,-112,74,50,50,-128,59,0,6,-128,71,0,12,-128,74,0,52,-112,73,50,6,-112,76,50,62,-128,73,0,6,-128,76,0,54,-112,74,50,10,-112,78,50,7,-112,59,50,52,-128,74,0,10,-128,78,0,7,-128,59,0,36,-112,68,50,0,-112,59,50,12,-112,80,50,0,-112,64,50,5,-112,76,50,52,-128,59,0,0,-128,68,0,12,-128,64,0,-127,70,-112,59,50,0,-112,68,50,14,-112,64,50,54,-128,68,0,0,-128,59,0,14,-128,64,0,8,-128,80,0,5,-128,76,0,10,-112,66,50,0,-112,57,50,12,-112,62,50,2,-112,78,50,0,-112,74,50,55,-128,57,0,0,-128,66,0,12,-128,62,0,2,-128,78,0,0,-128,74,0,-127,75,-112,66,50,0,-112,57,50,12,-112,62,50,57,-128,57,0,0,-128,66,0,12,-128,62,0,22,-112,68,50,0,-112,59,50,12,-112,80,50,2,-112,64,50,3,-112,76,50,52,-128,68,0,0,-128,59,0,13,-128,64,0,-127,68,-112,68,50,0,-112,59,50,14,-112,64,50,54,-128,59,0,0,-128,68,0,14,-128,64,0,8,-128,80,0,5,-128,76,0,10,-112,66,50,0,-112,57,50,12,-112,62,50,0,-112,78,50,4,-112,74,50,53,-128,66,0,0,-128,57,0,12,-128,62,0,0,-128,78,0,4,-128,74,0,-127,73,-112,57,50,0,-112,66,50,12,-112,62,50,44,-112,76,50,8,-112,80,50,5,-128,66,0,0,-128,57,0,12,-128,62,0,22,-112,69,50,0,-112,59,50,12,-112,81,50,2,-112,78,50,0,-112,66,50,5,-112,68,50,0,-112,59,50,4,-128,76,0,8,-112,80,50,0,-128,80,0,2,-112,64,50,3,-112,76,50,34,-128,69,0,0,-128,59,0,12,-128,81,0,2,-128,78,0,0,-128,66,0,5,-128,68,0,0,-128,59,0,13,-128,64,0,-127,50,-112,59,50,0,-112,68,50,12,-112,80,50,2,-112,64,50,2,-112,76,50,52,-128,68,0,0,-128,59,0,12,-128,80,0,2,-128,64,0,2,-128,76,0,20,-112,62,50,4,-128,80,0,5,-128,76,0,3,-112,66,50,0,-112,78,50,0,-112,57,50,4,-112,74,50,53,-128,62,0,12,-128,57,0,0,-128,78,0,0,-128,66,0,4,-128,74,0,-127,71,-112,66,50,12,-112,81,50,2,-112,57,50,0,-112,69,50,4,-112,78,50,51,-128,66,0,12,-128,81,0,2,-128,69,0,0,-128,57,0,4,-128,78,0,16,-112,61,50,12,-112,64,50,0,-112,76,50,4,-112,57,50,3,-112,73,50,50,-128,61,0,12,-128,76,0,0,-128,64,0,4,-128,57,0,3,-128,73,0,-127,68,-112,61,50,12,-112,57,50,0,-112,64,50,4,-112,73,50,0,-112,76,50,53,-128,61,0,12,-128,64,0,0,-128,57,0,4,-128,76,0,0,-128,73,0,18,-112,61,50,13,-112,64,50,0,-112,57,50,2,-112,73,50,0,-112,76,50,53,-128,61,0,13,-128,64,0,0,-128,57,0,2,-128,76,0,0,-128,73,0,-127,71,-112,61,50,13,-112,64,50,0,-112,57,50,2,-112,73,50,0,-112,76,50,53,-128,61,0,13,-128,64,0,0,-128,57,0,2,-128,76,0,0,-128,73,0,20,-112,71,50,12,-112,68,50,2,-112,65,50,3,-112,77,50,63,-128,68,0,5,-128,77,0,80,-128,71,0,13,-128,65,0,100,-112,71,50,12,-112,68,50,2,-112,65,50,2,-112,77,50,52,-128,71,0,12,-128,68,0,2,-128,65,0,2,-128,77,0,20,-112,65,50,12,-112,71,50,2,-112,68,50,0,-112,77,50,55,-128,65,0,12,-128,71,0,2,-128,68,0,0,-128,77,0,-127,75,-112,65,50,10,-112,68,50,2,-112,71,50,2,-112,77,50,55,-128,65,0,10,-128,68,0,2,-128,71,0,2,-128,77,0,20,-112,73,50,12,-112,69,50,2,-112,66,50,3,-112,78,50,-127,20,-128,73,0,12,-128,69,0,2,-128,66,0,3,-128,78,0,97,-112,73,50,14,-112,69,50,0,-112,66,50,2,-112,78,50,52,-128,73,0,14,-128,66,0,0,-128,69,0,2,-128,78,0,20,-112,66,50,12,-112,69,50,0,-112,73,50,4,-112,78,50,53,-128,66,0,12,-128,69,0,0,-128,73,0,4,-128,78,0,-127,73,-112,66,50,12,-112,73,50,0,-112,69,50,2,-112,78,50,55,-128,66,0,12,-128,69,0,0,-128,73,0,2,-128,78,0,20,-112,73,50,12,-112,71,50,2,-112,80,50,0,-112,65,50,-127,36,-128,65,0,0,-128,80,0,83,-128,73,0,12,-128,71,0,6,-112,73,50,12,-112,71,50,2,-112,65,50,2,-112,80,50,52,-128,73,0,12,-128,71,0,2,-128,65,0,2,-128,80,0,20,-112,65,50,12,-112,71,50,0,-112,73,50,4,-112,80,50,53,-128,65,0,12,-128,73,0,0,-128,71,0,4,-128,80,0,-127,71,-112,65,50,12,-112,71,50,2,-112,73,50,4,-112,80,50,51,-128,65,0,12,-128,71,0,2,-128,73,0,4,-128,80,0,18,-112,78,50,4,-112,73,50,10,-112,69,50,9,-112,81,50,-124,110,-128,78,0,4,-128,73,0,10,-128,69,0,9,-128,81,0,102,-112,62,50,12,-112,83,50,0,-112,71,50,5,-112,80,50,52,-128,62,0,12,-128,71,0,0,-128,83,0,5,-128,80,0,-127,65,-112,62,50,12,-112,81,50,2,-112,69,50,2,-112,78,50,52,-128,62,0,12,-128,81,0,2,-128,69,0,2,-128,78,0,20,-112,62,50,12,-112,66,50,2,-112,74,50,0,-112,78,50,55,-128,62,0,12,-128,66,0,2,-128,78,0,0,-128,74,0,-127,75,-112,62,50,10,-112,81,50,2,-112,66,50,2,-112,78,50,55,-128,62,0,10,-128,81,0,2,-128,66,0,2,-128,78,0,20,-112,64,50,12,-112,73,50,2,-112,57,50,3,-112,76,50,52,-128,64,0,12,-128,73,0,2,-128,57,0,3,-128,76,0,-127,65,-112,64,50,14,-112,64,50,0,-112,57,50,2,-112,73,50,52,-128,64,0,14,-128,57,0,0,-128,64,0,2,-128,73,0,20,-112,64,50,12,-112,57,50,0,-112,64,50,4,-112,73,50,53,-128,64,0,12,-128,64,0,0,-128,57,0,4,-128,73,0,-127,73,-112,57,50,12,-112,64,50,0,-112,66,50,2,-112,74,50,55,-128,57,0,12,-128,66,0,0,-128,64,0,2,-128,74,0,20,-112,62,50,12,-112,68,50,2,-112,59,50,0,-112,76,50,55,-128,62,0,12,-128,68,0,2,-128,76,0,0,-128,59,0,-127,68,-112,62,50,12,-112,66,50,2,-112,59,50,2,-112,74,50,52,-128,62,0,12,-128,66,0,2,-128,59,0,2,-128,74,0,20,-112,62,50,12,-112,56,50,0,-112,64,50,4,-112,73,50,53,-128,62,0,12,-128,64,0,0,-128,56,0,4,-128,73,0,-127,71,-112,62,50,12,-112,62,50,2,-112,56,50,4,-112,71,50,51,-128,62,0,12,-128,62,0,2,-128,56,0,4,-128,71,0,24,-112,61,50,6,-112,61,50,2,-112,69,50,0,-112,57,50,60,-128,61,0,6,-128,61,0,2,-128,57,0,0,-128,69,0,0,-1,47,0,77,84,114,107,0,0,4,107,0,-1,3,5,83,99,105,102,105,0,-63,99,2,-111,80,50,0,-111,76,50,6,-111,64,50,0,-111,68,50,-126,94,-127,76,0,0,-127,80,0,6,-127,68,0,0,-127,64,0,12,-111,74,50,6,-111,62,50,1,-111,78,50,6,-111,66,50,56,-127,74,0,6,-127,62,0,1,-127,78,0,6,-127,66,0,-126,60,-111,76,50,6,-111,64,50,4,-111,80,50,6,-111,68,50,-126,85,-127,76,0,6,-127,64,0,4,-127,80,0,6,-127,68,0,7,-111,74,50,6,-111,78,50,0,-111,62,50,6,-111,66,50,42,-127,78,0,6,-127,66,0,9,-127,74,0,6,-127,62,0,-127,127,-111,80,50,6,-111,68,50,7,-111,76,50,6,-111,64,50,16,-111,81,50,6,-111,69,50,12,-111,78,50,6,-111,66,50,10,-127,80,0,6,-127,68,0,7,-127,76,0,6,-127,64,0,16,-127,81,0,6,-127,69,0,12,-127,78,0,6,-127,66,0,-127,64,-111,80,50,4,-111,76,50,2,-111,68,50,4,-111,64,50,59,-127,80,0,4,-127,76,0,2,-127,68,0,4,-127,64,0,22,-111,74,50,6,-111,62,50,2,-111,78,50,6,-111,66,50,55,-127,74,0,6,-127,62,0,2,-127,78,0,6,-127,66,0,-127,86,-111,81,50,4,-111,78,50,2,-111,69,50,4,-111,66,50,59,-127,81,0,4,-127,78,0,2,-127,69,0,4,-127,66,0,16,-111,76,50,6,-111,64,50,0,-111,80,50,6,-111,68,50,57,-127,76,0,6,-127,80,0,0,-127,64,0,6,-127,68,0,-123,67,-111,76,50,0,-111,80,50,6,-111,68,50,0,-111,64,50,-126,94,-127,76,0,0,-127,80,0,6,-127,68,0,0,-127,64,0,12,-111,74,50,6,-111,62,50,1,-111,78,50,6,-111,66,50,56,-127,74,0,6,-127,62,0,1,-127,78,0,6,-127,66,0,-126,60,-111,76,50,6,-111,64,50,4,-111,80,50,6,-111,68,50,-126,85,-127,76,0,6,-127,64,0,4,-127,80,0,6,-127,68,0,7,-111,74,50,6,-111,78,50,0,-111,62,50,6,-111,66,50,42,-127,78,0,6,-127,66,0,9,-127,74,0,6,-127,62,0,-127,127,-111,80,50,6,-111,68,50,7,-111,76,50,6,-111,64,50,16,-111,81,50,6,-111,69,50,12,-111,78,50,6,-111,66,50,10,-127,80,0,6,-127,68,0,7,-127,76,0,6,-127,64,0,16,-127,81,0,6,-127,69,0,12,-127,78,0,6,-127,66,0,-127,64,-111,80,50,4,-111,76,50,2,-111,68,50,4,-111,64,50,59,-127,80,0,4,-127,76,0,2,-127,68,0,4,-127,64,0,22,-111,74,50,6,-111,62,50,2,-111,78,50,6,-111,66,50,55,-127,74,0,6,-127,62,0,2,-127,78,0,6,-127,66,0,-127,86,-111,81,50,4,-111,78,50,2,-111,69,50,4,-111,66,50,59,-127,81,0,4,-127,78,0,2,-127,69,0,4,-127,66,0,16,-111,73,50,6,-111,76,50,0,-111,61,50,6,-111,64,50,57,-127,73,0,6,-127,76,0,0,-127,61,0,6,-127,64,0,-123,54,-111,68,50,6,-111,56,50,9,-111,73,50,6,-111,61,50,-125,48,-127,68,0,6,-127,56,0,9,-127,73,0,6,-127,61,0,-127,78,-111,68,50,6,-111,56,50,8,-111,73,50,6,-111,61,50,49,-127,68,0,6,-127,56,0,8,-127,73,0,6,-127,61,0,18,-111,73,50,4,-111,69,50,2,-111,61,50,4,-111,57,50,-125,59,-127,73,0,4,-127,69,0,2,-127,61,0,4,-127,57,0,-127,64,-111,69,50,6,-111,57,50,3,-111,73,50,6,-111,61,50,54,-127,69,0,6,-127,57,0,3,-127,73,0,6,-127,61,0,41,-111,73,50,5,-111,68,50,1,-111,61,50,5,-111,56,50,-125,58,-127,73,0,5,-127,68,0,1,-127,61,0,5,-127,56,0,-127,66,-111,68,50,6,-111,56,50,0,-111,73,50,6,-111,61,50,57,-127,68,0,6,-127,56,0,0,-127,73,0,6,-127,61,0,16,-111,69,50,6,-111,57,50,10,-111,73,50,6,-111,61,50,-124,111,-127,69,0,6,-127,57,0,106,-127,73,0,6,-127,61,0,7,-111,71,50,2,-111,80,50,4,-111,59,50,2,-111,68,50,60,-127,71,0,2,-127,80,0,4,-127,59,0,2,-127,68,0,-126,38,-111,71,50,6,-111,59,50,7,-111,80,50,6,-111,68,50,50,-127,71,0,6,-127,59,0,7,-127,80,0,6,-127,68,0,-126,40,-111,81,50,0,-111,73,50,6,-111,69,50,0,-111,61,50,62,-127,73,0,0,-127,81,0,6,-127,69,0,0,-127,61,0,-126,58,-111,81,50,0,-111,73,50,6,-111,61,50,0,-111,69,50,62,-127,73,0,0,-127,81,0,6,-127,69,0,0,-127,61,0,-126,60,-111,71,50,6,-111,59,50,1,-111,76,50,6,-111,64,50,56,-127,71,0,6,-127,59,0,1,-127,76,0,6,-127,64,0,-126,50,-111,71,50,6,-111,59,50,2,-111,76,50,6,-111,64,50,55,-127,71,0,6,-127,59,0,2,-127,76,0,6,-127,64,0,-126,48,-111,73,50,6,-111,81,50,0,-111,61,50,6,-111,69,50,57,-127,73,0,6,-127,61,0,0,-127,81,0,6,-127,69,0,0,-1,47,0,77,84,114,107,0,0,3,-116,0,-1,3,6,86,105,111,108,105,110,0,-62,40,0,-110,92,50,-126,100,-126,92,0,12,-110,86,50,12,-110,90,50,56,-126,86,0,12,-126,90,0,-126,49,-110,88,50,14,-110,92,50,-126,86,-126,88,0,14,-126,92,0,12,-110,86,50,12,-110,90,50,56,-126,86,0,12,-126,90,0,-127,111,-110,88,50,35,-110,92,50,32,-110,93,50,2,-126,88,0,12,-110,90,50,22,-126,92,0,32,-126,93,0,14,-126,90,0,-127,68,-110,92,50,13,-110,88,50,55,-126,92,0,13,-126,88,0,22,-110,90,50,12,-110,86,50,56,-126,90,0,12,-126,86,0,-127,74,-110,93,50,13,-110,90,50,55,-126,93,0,13,-126,90,0,22,-110,92,50,14,-110,88,50,54,-126,92,0,14,-126,88,0,54,-110,80,50,0,-110,83,50,68,-126,83,0,0,-126,80,0,56,-110,81,50,10,-110,85,50,59,-126,81,0,10,-126,85,0,46,-110,83,50,4,-110,86,50,64,-126,83,0,4,-126,86,0,62,-110,85,50,0,-110,88,50,48,-126,88,0,20,-126,85,0,56,-110,86,50,0,-110,90,50,68,-126,86,0,0,-126,90,0,58,-110,92,50,12,-110,88,50,-126,88,-126,92,0,12,-126,88,0,13,-110,90,50,12,-110,86,50,56,-126,90,0,12,-126,86,0,-126,49,-110,92,50,14,-110,88,50,-126,86,-126,92,0,14,-126,88,0,12,-110,90,50,10,-110,86,50,59,-126,90,0,10,-126,86,0,-127,114,-110,88,50,35,-110,92,50,32,-110,93,50,2,-126,88,0,11,-110,90,50,6,-110,88,50,18,-126,92,0,32,-126,93,0,12,-126,90,0,-127,70,-110,92,50,13,-110,88,50,55,-126,92,0,13,-126,88,0,14,-126,88,0,8,-110,90,50,10,-110,86,50,59,-126,90,0,10,-126,86,0,-127,76,-110,93,50,13,-110,90,50,55,-126,93,0,13,-126,90,0,22,-110,88,50,12,-110,85,50,56,-126,88,0,12,-126,85,0,-127,72,-110,85,50,12,-110,88,50,57,-126,85,0,12,-126,88,0,22,-110,85,50,14,-110,88,50,54,-126,85,0,14,-126,88,0,-127,72,-110,85,50,13,-110,88,50,55,-126,85,0,13,-126,88,0,22,-110,80,50,14,-110,89,50,-127,22,-126,80,0,14,-126,89,0,100,-110,80,50,13,-110,89,50,55,-126,80,0,13,-126,89,0,22,-110,80,50,12,-110,89,50,56,-126,80,0,12,-126,89,0,-127,76,-110,80,50,6,-110,89,50,62,-126,80,0,6,-126,89,0,28,-110,81,50,14,-110,90,50,-127,22,-126,81,0,14,-126,90,0,100,-110,81,50,13,-110,90,50,55,-126,81,0,13,-126,90,0,22,-110,81,50,12,-110,90,50,56,-126,81,0,12,-126,90,0,-127,76,-110,81,50,2,-110,90,50,66,-126,81,0,2,-126,90,0,32,-110,83,50,14,-110,92,50,-127,22,-126,83,0,14,-126,92,0,100,-110,83,50,13,-110,92,50,55,-126,83,0,13,-126,92,0,22,-110,83,50,12,-110,92,50,56,-126,83,0,12,-126,92,0,-127,74,-110,83,50,6,-110,92,50,62,-126,83,0,6,-126,92,0,30,-110,85,50,40,-110,93,50,-124,92,-126,85,0,40,-126,93,0,84,-110,92,50,12,-110,95,50,56,-126,92,0,12,-126,95,0,-127,70,-110,90,50,13,-110,93,50,55,-126,90,0,13,-126,93,0,22,-110,86,50,12,-110,90,50,56,-126,86,0,12,-126,90,0,-127,76,-110,90,50,12,-110,93,50,57,-126,90,0,12,-126,93,0,22,-110,85,50,14,-110,88,50,54,-126,85,0,14,-126,88,0,-127,68,-110,76,50,13,-110,85,50,55,-126,76,0,13,-126,85,0,22,-110,76,50,10,-110,85,50,59,-126,76,0,10,-126,85,0,-127,78,-110,78,50,12,-110,86,50,57,-126,78,0,12,-126,86,0,22,-110,80,50,12,-110,88,50,56,-126,80,0,12,-126,88,0,-127,70,-110,78,50,15,-110,86,50,53,-126,78,0,15,-126,86,0,20,-110,76,50,10,-110,85,50,59,-126,76,0,10,-126,85,0,-127,76,-110,74,50,13,-110,83,50,55,-126,74,0,13,-126,83,0,40,-110,81,50,2,-110,73,50,66,-126,81,0,2,-126,73,0,0,-1,47,0,77,84,114,107,0,0,0,-107,0,-1,3,5,67,101,108,108,111,0,-61,42,-123,124,-109,52,50,-126,100,-125,52,0,-125,28,-109,52,50,-126,100,-125,52,0,-125,28,-109,52,50,68,-125,52,0,-123,60,-109,52,50,-126,100,-125,52,0,-125,28,-109,52,50,-126,100,-125,52,0,-125,28,-109,52,50,18,-109,52,50,-126,82,-125,52,0,18,-125,52,0,-125,10,-109,57,50,-126,4,-125,57,0,124,-109,52,50,-126,4,-125,52,0,124,-109,56,50,-126,4,-125,56,0,-125,124,-109,54,50,-126,100,-125,54,0,-125,28,-109,53,50,-126,100,-125,53,0,-125,28,-109,54,50,-126,100,-125,54,0,28,-109,54,50,68,-125,54,0,0,-1,47,0,77,84,114,107,0,0,0,107,0,-1,3,10,56,45,66,105,116,32,83,105,110,101,0,-60,82,2,-108,64,50,-105,4,-124,64,0,124,-108,64,50,-111,100,-124,64,0,-98,24,-108,71,50,10,-108,62,50,-123,91,-124,71,0,10,-124,62,0,10,-108,64,50,9,-108,69,50,-123,92,-124,64,0,9,-124,69,0,17,-108,62,50,11,-108,68,50,-123,90,-124,62,0,11,-124,68,0,26,-108,61,50,10,-108,69,50,59,-124,61,0,10,-124,69,0,0,-1,47,0,77,84,114,107,0,0,1,30,0,-1,3,14,56,45,66,105,116,32,84,114,105,97,110,103,108,101,0,-59,87,2,-107,52,50,-126,100,-123,52,0,-125,28,-107,52,50,-126,100,-123,52,0,-125,28,-107,52,50,-126,100,-123,52,0,-125,28,-107,52,50,68,-123,52,0,-123,60,-107,52,50,-126,100,-123,52,0,-125,28,-107,52,50,-126,100,-123,52,0,-125,28,-107,52,50,-126,100,-123,52,0,-125,28,-107,57,50,-126,100,-123,57,0,28,-107,52,50,-126,100,-123,52,0,28,-107,56,50,-126,100,-123,56,0,-125,28,-107,54,50,-126,100,-123,54,0,-125,28,-107,53,50,-126,100,-123,53,0,-125,28,-107,54,50,-126,100,-123,54,0,28,-107,54,50,68,-123,54,0,-126,54,-107,74,50,6,-107,62,50,62,-123,74,0,6,-123,62,0,-126,45,-107,74,50,2,-107,62,50,66,-123,74,0,2,-123,62,0,-126,62,-107,76,50,0,-107,69,50,68,-123,69,0,0,-123,76,0,-126,52,-107,69,50,6,-107,76,50,62,-123,69,0,6,-123,76,0,-126,56,-107,68,50,2,-107,74,50,66,-123,68,0,2,-123,74,0,-126,56,-107,74,50,2,-107,68,50,66,-123,74,0,2,-123,68,0,-126,62,-107,76,50,2,-107,69,50,12,-107,57,50,54,-123,76,0,2,-123,69,0,12,-123,57,0,0,-1,47,0,77,84,114,107,0,0,1,13,0,-1,3,9,80,105,122,122,105,99,97,116,111,0,-58,45,8,-106,52,50,0,-106,52,50,-127,36,-122,52,0,0,-122,52,0,-124,92,-106,52,50,0,-106,52,50,-127,36,-122,52,0,0,-122,52,0,-124,92,-106,52,50,0,-106,52,50,-127,36,-122,52,0,0,-122,52,0,-124,92,-106,52,50,0,-106,52,50,-127,36,-122,52,0,0,-122,52,0,-124,92,-106,52,50,0,-106,52,50,-127,36,-122,52,0,0,-122,52,0,-124,92,-106,52,50,0,-106,52,50,-127,36,-122,52,0,0,-122,52,0,-124,92,-106,52,50,0,-106,52,50,-127,36,-122,52,0,0,-122,52,0,-124,92,-106,57,50,0,-106,57,50,-127,36,-122,57,0,0,-122,57,0,-127,92,-106,52,50,0,-106,52,50,-127,36,-122,52,0,0,-122,52,0,-127,92,-106,56,50,0,-106,56,50,-127,36,-122,56,0,0,-122,56,0,-124,92,-106,54,50,0,-106,54,50,-127,36,-122,54,0,0,-122,54,0,-124,92,-106,53,50,0,-106,53,50,-127,36,-122,53,0,0,-122,53,0,-124,92,-106,54,50,0,-106,54,50,-126,100,-122,54,0,0,-122,54,0,28,-106,54,50,0,-106,54,50,68,-122,54,0,0,-122,54,0,0,-1,47,0,77,84,114,107,0,0,0,-89,0,-1,3,5,70,108,117,116,101,0,-57,73,-117,78,-105,92,50,30,-105,93,50,4,-105,88,50,20,-105,90,50,14,-121,92,0,13,-121,88,0,16,-121,93,0,24,-121,90,0,-105,6,-105,92,50,30,-105,93,50,4,-105,88,50,20,-105,90,50,14,-121,92,0,13,-121,88,0,16,-121,93,0,24,-121,90,0,-117,32,-105,89,50,2,-105,92,50,-123,98,-121,89,0,2,-121,92,0,54,-105,90,50,4,-105,93,50,-123,96,-121,90,0,4,-121,93,0,30,-105,92,50,0,-105,95,50,-123,100,-121,95,0,0,-121,92,0,14,-105,93,50,8,-105,90,50,-123,92,-121,93,0,8,-121,90,0,32,-105,92,50,4,-105,95,50,64,-121,92,0,4,-121,95,0,0,-1,47,0,77,84,114,107,0,0,1,5,0,-1,3,13,82,97,103,116,105,109,101,32,80,105,97,110,111,0,-56,3,8,-104,52,50,0,-104,52,50,118,-120,52,0,0,-120,52,0,-123,10,-104,52,50,0,-104,52,50,118,-120,52,0,0,-120,52,0,-123,10,-104,52,50,0,-104,52,50,118,-120,52,0,0,-120,52,0,-123,10,-104,52,50,0,-104,52,50,118,-120,52,0,0,-120,52,0,-123,10,-104,52,50,0,-104,52,50,118,-120,52,0,0,-120,52,0,-123,10,-104,52,50,0,-104,52,50,118,-120,52,0,0,-120,52,0,-123,10,-104,52,50,0,-104,52,50,118,-120,52,0,0,-120,52,0,-123,10,-104,57,50,0,-104,57,50,118,-120,57,0,0,-120,57,0,-126,10,-104,52,50,0,-104,52,50,118,-120,52,0,0,-120,52,0,-126,10,-104,56,50,0,-104,56,50,118,-120,56,0,0,-120,56,0,-123,10,-104,54,50,0,-104,54,50,118,-120,54,0,0,-120,54,0,-123,10,-104,53,50,0,-104,53,50,118,-120,53,0,0,-120,53,0,-123,10,-104,54,50,0,-104,54,50,-126,54,-120,54,0,0,-120,54,0,74,-104,54,50,0,-104,54,50,48,-120,54,0,0,-120,54,0,0,-1,47,0,77,84,114,107,0,0,8,0,0,-1,3,11,74,97,122,122,32,71,117,105,116,97,114,0,-54,26,-126,1,-102,68,50,0,-102,59,50,14,-102,64,50,54,-118,59,0,0,-118,68,0,14,-118,64,0,22,-102,66,50,0,-102,57,50,12,-102,62,50,2,-102,78,50,0,-102,74,50,55,-118,66,0,0,-118,57,0,12,-118,62,0,2,-118,74,0,0,-118,78,0,-127,75,-102,66,50,0,-102,57,50,12,-102,62,50,57,-118,66,0,0,-118,57,0,12,-118,62,0,22,-102,59,50,0,-102,68,50,12,-102,80,50,2,-102,64,50,3,-102,76,50,52,-118,68,0,0,-118,59,0,13,-118,64,0,-127,68,-102,68,50,0,-102,59,50,14,-102,64,50,54,-118,68,0,0,-118,59,0,14,-118,64,0,8,-118,80,0,5,-118,76,0,10,-102,66,50,0,-102,57,50,12,-102,62,50,0,-102,78,50,4,-102,74,50,53,-118,57,0,0,-118,66,0,12,-118,78,0,0,-118,62,0,4,-118,74,0,-127,73,-102,66,50,0,-102,57,50,12,-102,62,50,44,-102,76,50,8,-102,80,50,5,-118,66,0,0,-118,57,0,12,-118,62,0,22,-102,69,50,0,-102,59,50,12,-102,81,50,2,-102,78,50,0,-102,66,50,9,-118,76,0,8,-118,80,0,38,-118,69,0,0,-118,59,0,12,-118,81,0,2,-118,66,0,0,-118,78,0,-127,68,-102,68,50,0,-102,59,50,12,-102,80,50,2,-102,64,50,2,-102,76,50,52,-118,59,0,0,-118,68,0,12,-118,80,0,2,-118,64,0,2,-118,76,0,20,-102,62,50,12,-102,57,50,0,-102,66,50,0,-102,78,50,4,-102,74,50,53,-118,62,0,12,-118,57,0,0,-118,78,0,0,-118,66,0,4,-118,74,0,-127,71,-102,66,50,12,-102,81,50,2,-102,57,50,0,-102,69,50,4,-102,78,50,51,-118,66,0,12,-118,81,0,2,-118,69,0,0,-118,57,0,4,-118,78,0,16,-102,64,50,12,-102,68,50,2,-102,80,50,2,-102,59,50,3,-102,76,50,50,-118,64,0,12,-118,68,0,2,-118,80,0,2,-118,59,0,3,-118,76,0,60,-102,71,50,0,-102,68,50,68,-118,71,0,0,-118,68,0,58,-102,73,50,0,-102,69,50,22,-102,59,50,46,-118,69,0,0,-118,73,0,22,-118,59,0,22,-102,59,50,6,-102,71,50,12,-102,74,50,50,-118,59,0,6,-118,71,0,12,-118,74,0,52,-102,73,50,6,-102,76,50,62,-118,73,0,6,-118,76,0,54,-102,74,50,10,-102,78,50,7,-102,59,50,52,-118,74,0,10,-118,78,0,7,-118,59,0,36,-102,68,50,0,-102,59,50,12,-102,80,50,0,-102,64,50,5,-102,76,50,52,-118,59,0,0,-118,68,0,12,-118,64,0,-127,70,-102,59,50,0,-102,68,50,14,-102,64,50,54,-118,68,0,0,-118,59,0,14,-118,64,0,8,-118,80,0,5,-118,76,0,10,-102,66,50,0,-102,57,50,12,-102,62,50,2,-102,78,50,0,-102,74,50,55,-118,57,0,0,-118,66,0,12,-118,62,0,2,-118,78,0,0,-118,74,0,-127,75,-102,66,50,0,-102,57,50,12,-102,62,50,57,-118,57,0,0,-118,66,0,12,-118,62,0,22,-102,68,50,0,-102,59,50,12,-102,80,50,2,-102,64,50,3,-102,76,50,52,-118,68,0,0,-118,59,0,13,-118,64,0,-127,68,-102,68,50,0,-102,59,50,14,-102,64,50,54,-118,59,0,0,-118,68,0,14,-118,64,0,8,-118,80,0,5,-118,76,0,10,-102,66,50,0,-102,57,50,12,-102,62,50,0,-102,78,50,4,-102,74,50,53,-118,66,0,0,-118,57,0,12,-118,62,0,0,-118,78,0,4,-118,74,0,-127,73,-102,57,50,0,-102,66,50,12,-102,62,50,44,-102,76,50,8,-102,80,50,5,-118,66,0,0,-118,57,0,12,-118,62,0,22,-102,69,50,0,-102,59,50,12,-102,81,50,2,-102,78,50,0,-102,66,50,5,-102,68,50,0,-102,59,50,4,-118,76,0,8,-102,80,50,0,-118,80,0,2,-102,64,50,3,-102,76,50,34,-118,69,0,0,-118,59,0,12,-118,81,0,2,-118,78,0,0,-118,66,0,5,-118,68,0,0,-118,59,0,13,-118,64,0,-127,50,-102,59,50,0,-102,68,50,12,-102,80,50,2,-102,64,50,2,-102,76,50,52,-118,68,0,0,-118,59,0,12,-118,80,0,2,-118,64,0,2,-118,76,0,20,-102,62,50,4,-118,80,0,5,-118,76,0,3,-102,66,50,0,-102,78,50,0,-102,57,50,4,-102,74,50,53,-118,62,0,12,-118,57,0,0,-118,78,0,0,-118,66,0,4,-118,74,0,-127,71,-102,66,50,12,-102,81,50,2,-102,57,50,0,-102,69,50,4,-102,78,50,51,-118,66,0,12,-118,81,0,2,-118,69,0,0,-118,57,0,4,-118,78,0,16,-102,61,50,12,-102,64,50,0,-102,76,50,4,-102,57,50,3,-102,73,50,50,-118,61,0,12,-118,76,0,0,-118,64,0,4,-118,57,0,3,-118,73,0,-127,68,-102,61,50,12,-102,57,50,0,-102,64,50,4,-102,73,50,0,-102,76,50,53,-118,61,0,12,-118,64,0,0,-118,57,0,4,-118,76,0,0,-118,73,0,18,-102,61,50,13,-102,64,50,0,-102,57,50,2,-102,73,50,0,-102,76,50,53,-118,61,0,13,-118,64,0,0,-118,57,0,2,-118,76,0,0,-118,73,0,-127,71,-102,61,50,13,-102,64,50,0,-102,57,50,2,-102,73,50,0,-102,76,50,53,-118,61,0,13,-118,64,0,0,-118,57,0,2,-118,76,0,0,-118,73,0,20,-102,71,50,12,-102,68,50,2,-102,65,50,3,-102,77,50,63,-118,68,0,5,-118,77,0,80,-118,71,0,13,-118,65,0,100,-102,71,50,12,-102,68,50,2,-102,65,50,2,-102,77,50,52,-118,71,0,12,-118,68,0,2,-118,65,0,2,-118,77,0,20,-102,65,50,12,-102,71,50,2,-102,68,50,0,-102,77,50,55,-118,65,0,12,-118,71,0,2,-118,68,0,0,-118,77,0,-127,75,-102,65,50,10,-102,68,50,2,-102,71,50,2,-102,77,50,55,-118,65,0,10,-118,68,0,2,-118,71,0,2,-118,77,0,20,-102,73,50,12,-102,69,50,2,-102,66,50,3,-102,78,50,-127,20,-118,73,0,12,-118,69,0,2,-118,66,0,3,-118,78,0,97,-102,73,50,14,-102,69,50,0,-102,66,50,2,-102,78,50,52,-118,73,0,14,-118,66,0,0,-118,69,0,2,-118,78,0,20,-102,66,50,12,-102,69,50,0,-102,73,50,4,-102,78,50,53,-118,66,0,12,-118,69,0,0,-118,73,0,4,-118,78,0,-127,73,-102,66,50,12,-102,73,50,0,-102,69,50,2,-102,78,50,55,-118,66,0,12,-118,69,0,0,-118,73,0,2,-118,78,0,20,-102,73,50,12,-102,71,50,2,-102,80,50,0,-102,65,50,-127,36,-118,65,0,0,-118,80,0,83,-118,73,0,12,-118,71,0,6,-102,73,50,12,-102,71,50,2,-102,65,50,2,-102,80,50,52,-118,73,0,12,-118,71,0,2,-118,65,0,2,-118,80,0,20,-102,65,50,12,-102,71,50,0,-102,73,50,4,-102,80,50,53,-118,65,0,12,-118,73,0,0,-118,71,0,4,-118,80,0,-127,71,-102,65,50,12,-102,71,50,2,-102,73,50,4,-102,80,50,51,-118,65,0,12,-118,71,0,2,-118,73,0,4,-118,80,0,18,-102,78,50,4,-102,73,50,10,-102,69,50,9,-102,81,50,-124,110,-118,78,0,4,-118,73,0,10,-118,69,0,9,-118,81,0,102,-102,62,50,12,-102,83,50,0,-102,71,50,5,-102,80,50,52,-118,62,0,12,-118,71,0,0,-118,83,0,5,-118,80,0,-127,65,-102,62,50,12,-102,81,50,2,-102,69,50,2,-102,78,50,52,-118,62,0,12,-118,81,0,2,-118,69,0,2,-118,78,0,20,-102,62,50,12,-102,66,50,2,-102,74,50,0,-102,78,50,55,-118,62,0,12,-118,66,0,2,-118,78,0,0,-118,74,0,-127,75,-102,62,50,10,-102,81,50,2,-102,66,50,2,-102,78,50,55,-118,62,0,10,-118,81,0,2,-118,66,0,2,-118,78,0,20,-102,64,50,12,-102,73,50,2,-102,57,50,3,-102,76,50,52,-118,64,0,12,-118,73,0,2,-118,57,0,3,-118,76,0,-127,65,-102,64,50,14,-102,64,50,0,-102,57,50,2,-102,73,50,52,-118,64,0,14,-118,57,0,0,-118,64,0,2,-118,73,0,20,-102,64,50,12,-102,57,50,0,-102,64,50,4,-102,73,50,53,-118,64,0,12,-118,64,0,0,-118,57,0,4,-118,73,0,-127,73,-102,57,50,12,-102,64,50,0,-102,66,50,2,-102,74,50,55,-118,57,0,12,-118,66,0,0,-118,64,0,2,-118,74,0,20,-102,62,50,12,-102,68,50,2,-102,59,50,0,-102,76,50,55,-118,62,0,12,-118,68,0,2,-118,76,0,0,-118,59,0,-127,68,-102,62,50,12,-102,66,50,2,-102,59,50,2,-102,74,50,52,-118,62,0,12,-118,66,0,2,-118,59,0,2,-118,74,0,20,-102,62,50,12,-102,56,50,0,-102,64,50,4,-102,73,50,53,-118,62,0,12,-118,64,0,0,-118,56,0,4,-118,73,0,-127,71,-102,62,50,12,-102,62,50,2,-102,56,50,4,-102,71,50,51,-118,62,0,12,-118,62,0,2,-118,56,0,4,-118,71,0,24,-102,61,50,6,-102,61,50,2,-102,69,50,0,-102,57,50,60,-118,61,0,6,-118,61,0,2,-118,57,0,0,-118,69,0,0,-1,47,0,77,84,114,107,0,0,5,70,0,-1,3,4,75,111,116,111,0,-53,107,10,-101,68,50,6,-101,80,50,4,-101,56,50,6,-101,68,50,-126,85,-117,68,0,6,-117,80,0,4,-117,56,0,6,-117,68,0,2,-101,66,50,6,-101,78,50,4,-101,54,50,6,-101,66,50,53,-117,66,0,6,-117,78,0,4,-117,54,0,6,-117,66,0,-126,49,-101,68,50,6,-101,80,50,4,-101,56,50,6,-101,68,50,-126,85,-117,68,0,6,-117,80,0,4,-117,56,0,6,-117,68,0,8,-101,66,50,6,-101,78,50,4,-101,54,50,6,-101,66,50,53,-117,66,0,6,-117,78,0,4,-117,54,0,6,-117,66,0,-126,49,-101,69,50,6,-101,81,50,4,-101,57,50,6,-101,69,50,53,-117,69,0,6,-117,81,0,4,-117,57,0,6,-117,69,0,-127,52,-101,68,50,6,-101,80,50,4,-101,56,50,6,-101,68,50,53,-117,68,0,6,-117,80,0,4,-117,56,0,6,-117,68,0,38,-101,66,50,6,-101,78,50,4,-101,54,50,6,-101,66,50,53,-117,66,0,6,-117,78,0,4,-117,54,0,6,-117,66,0,-127,52,-101,69,50,6,-101,81,50,4,-101,57,50,6,-101,69,50,53,-117,69,0,6,-117,81,0,4,-117,57,0,6,-117,69,0,33,-101,68,50,6,-101,80,50,4,-101,56,50,6,-101,68,50,-125,53,-117,68,0,6,-117,80,0,4,-117,56,0,6,-117,68,0,-126,36,-101,68,50,6,-101,80,50,4,-101,56,50,6,-101,68,50,-126,85,-117,68,0,6,-117,80,0,4,-117,56,0,6,-117,68,0,27,-101,66,50,6,-101,78,50,4,-101,54,50,6,-101,66,50,53,-117,66,0,6,-117,78,0,4,-117,54,0,6,-117,66,0,-126,39,-101,68,50,6,-101,80,50,4,-101,56,50,6,-101,68,50,-126,85,-117,68,0,6,-117,80,0,4,-117,56,0,6,-117,68,0,8,-101,66,50,6,-101,78,50,4,-101,54,50,6,-101,66,50,53,-117,66,0,6,-117,78,0,4,-117,54,0,6,-117,66,0,-126,41,-101,69,50,6,-101,81,50,4,-101,57,50,6,-101,69,50,53,-117,69,0,6,-117,81,0,4,-117,57,0,6,-117,69,0,-127,58,-101,68,50,6,-101,80,50,4,-101,56,50,6,-101,68,50,53,-117,68,0,6,-117,80,0,4,-117,56,0,6,-117,68,0,43,-101,66,50,6,-101,78,50,4,-101,54,50,6,-101,66,50,53,-117,66,0,6,-117,78,0,4,-117,54,0,6,-117,66,0,-127,44,-101,69,50,6,-101,81,50,4,-101,57,50,6,-101,69,50,53,-117,69,0,6,-117,81,0,4,-117,57,0,6,-117,69,0,43,-101,64,50,6,-101,76,50,4,-101,52,50,6,-101,64,50,53,-117,64,0,6,-117,76,0,4,-117,52,0,6,-117,64,0,-127,56,-101,61,50,6,-101,73,50,10,-101,61,50,53,-117,61,0,6,-117,73,0,10,-117,61,0,36,-101,61,50,6,-101,73,50,10,-101,61,50,53,-117,61,0,6,-117,73,0,10,-117,61,0,-127,52,-101,61,50,6,-101,73,50,10,-101,61,50,53,-117,61,0,6,-117,73,0,10,-117,61,0,38,-101,61,50,6,-101,73,50,10,-101,61,50,-127,21,-117,61,0,6,-117,73,0,10,-117,61,0,90,-101,61,50,6,-101,73,50,10,-101,61,50,53,-117,61,0,6,-117,73,0,10,-117,61,0,31,-101,61,50,6,-101,73,50,10,-101,61,50,53,-117,61,0,6,-117,73,0,10,-117,61,0,-127,52,-101,61,50,6,-101,73,50,10,-101,61,50,53,-117,61,0,6,-117,73,0,10,-117,61,0,36,-101,61,50,6,-101,73,50,10,-101,61,50,-127,21,-117,61,0,6,-117,73,0,10,-117,61,0,86,-101,61,50,6,-101,73,50,10,-101,61,50,53,-117,61,0,6,-117,73,0,10,-117,61,0,27,-101,61,50,6,-101,73,50,10,-101,61,50,53,-117,61,0,6,-117,73,0,10,-117,61,0,-127,54,-101,61,50,6,-101,73,50,10,-101,61,50,53,-117,61,0,6,-117,73,0,10,-117,61,0,36,-101,61,50,6,-101,73,50,10,-101,61,50,-127,21,-117,61,0,6,-117,73,0,10,-117,61,0,100,-101,61,50,6,-101,73,50,10,-101,61,50,53,-117,61,0,6,-117,73,0,10,-117,61,0,33,-101,61,50,6,-101,73,50,10,-101,61,50,53,-117,61,0,6,-117,73,0,10,-117,61,0,-127,47,-101,61,50,6,-101,73,50,10,-101,61,50,53,-117,61,0,6,-117,73,0,10,-117,61,0,38,-101,69,50,6,-101,81,50,4,-101,57,50,6,-101,69,50,-124,21,-117,69,0,6,-117,81,0,4,-117,57,0,6,-117,69,0,-127,71,-101,71,50,6,-101,83,50,4,-101,59,50,6,-101,71,50,53,-117,71,0,6,-117,83,0,4,-117,59,0,6,-117,71,0,-127,56,-101,69,50,6,-101,81,50,4,-101,57,50,6,-101,69,50,53,-117,69,0,6,-117,81,0,4,-117,57,0,6,-117,69,0,38,-101,66,50,6,-101,78,50,10,-101,66,50,53,-117,66,0,6,-117,78,0,10,-117,66,0,-127,49,-101,69,50,6,-101,81,50,4,-101,57,50,6,-101,69,50,53,-117,69,0,6,-117,81,0,4,-117,57,0,6,-117,69,0,44,-101,64,50,6,-101,76,50,10,-101,64,50,53,-117,64,0,6,-117,76,0,10,-117,64,0,-127,51,-101,61,50,6,-101,73,50,10,-101,61,50,53,-117,61,0,6,-117,73,0,10,-117,61,0,38,-101,61,50,6,-101,73,50,10,-101,61,50,53,-117,61,0,6,-117,73,0,10,-117,61,0,-127,47,-101,62,50,6,-101,74,50,10,-101,62,50,53,-117,62,0,6,-117,74,0,10,-117,62,0,36,-101,64,50,6,-101,76,50,10,-101,64,50,53,-117,64,0,6,-117,76,0,10,-117,64,0,-127,51,-101,62,50,6,-101,74,50,10,-101,62,50,53,-117,62,0,6,-117,74,0,10,-117,62,0,44,-101,61,50,6,-101,73,50,10,-101,61,50,53,-117,61,0,6,-117,73,0,10,-117,61,0,-127,49,-101,59,50,6,-101,71,50,10,-101,59,50,53,-117,59,0,6,-117,71,0,10,-117,59,0,38,-101,57,50,6,-101,69,50,10,-101,57,50,-127,117,-117,57,0,6,-117,69,0,10,-117,57,0,0,-1,47,0,};
	public static final byte [] loser = {77,84,104,100,0,0,0,6,0,1,0,5,1,-128,77,84,114,107,0,0,0,19,0,-1,88,4,4,2,24,8,0,-1,81,3,4,-47,-115,0,-1,47,0,77,84,114,107,0,0,0,97,0,-1,3,12,83,109,111,111,116,104,32,83,121,110,116,104,0,-64,80,-127,64,-112,67,50,-127,64,-128,67,0,0,-112,67,50,-127,64,-128,67,0,0,-112,67,50,-127,64,-128,67,0,0,-112,63,50,-122,0,-128,63,0,-127,64,-112,65,50,-127,64,-128,65,0,0,-112,65,50,-127,64,-128,65,0,0,-112,65,50,-127,64,-128,65,0,0,-112,62,50,-116,0,-128,62,0,0,-1,47,0,77,84,114,107,0,0,0,89,0,-1,3,4,66,97,115,115,0,-63,32,-127,64,-111,31,50,-127,64,-127,31,0,0,-111,31,50,-127,64,-127,31,0,0,-111,31,50,-127,64,-127,31,0,0,-111,27,50,-122,0,-127,27,0,-127,64,-111,29,50,-127,64,-127,29,0,0,-111,29,50,-127,64,-127,29,0,0,-111,29,50,-127,64,-127,29,0,0,-111,26,50,-116,0,-127,26,0,0,-1,47,0,77,84,114,107,0,0,0,-101,0,-1,3,6,86,105,111,108,105,110,0,-62,40,-127,64,-110,55,50,0,-110,67,50,-127,64,-126,67,0,0,-126,55,0,0,-110,67,50,0,-110,55,50,-127,64,-126,55,0,0,-126,67,0,0,-110,67,50,0,-110,55,50,-127,64,-126,55,0,0,-126,67,0,0,-110,63,50,0,-110,51,50,-122,0,-126,51,0,0,-126,63,0,-127,64,-110,53,50,0,-110,65,50,-127,64,-126,53,0,0,-126,65,0,0,-110,65,50,0,-110,53,50,-127,64,-126,53,0,0,-126,65,0,0,-110,65,50,0,-110,53,50,-127,64,-126,53,0,0,-126,65,0,0,-110,62,50,0,-110,50,50,-116,0,-126,50,0,0,-126,62,0,0,-1,47,0,77,84,114,107,0,0,0,90,0,-1,3,5,67,101,108,108,111,0,-61,42,-127,64,-109,43,50,-127,64,-125,43,0,0,-109,43,50,-127,64,-125,43,0,0,-109,43,50,-127,64,-125,43,0,0,-109,39,50,-122,0,-125,39,0,-127,64,-109,41,50,-127,64,-125,41,0,0,-109,41,50,-127,64,-125,41,0,0,-109,41,50,-127,64,-125,41,0,0,-109,38,50,-116,0,-125,38,0,0,-1,47,0,};
	public static final byte [] click = {77,84,104,100,0,0,0,6,0,1,0,2,1,-128,77,84,114,107,0,0,0,19,0,-1,88,4,4,2,24,8,0,-1,81,3,8,82,-82,0,-1,47,0,77,84,114,107,0,0,0,27,0,-1,3,8,68,114,117,109,32,75,105,116,0,-55,0,0,-103,85,50,96,-119,85,0,0,-1,47,0,};	
	
	//plays the sound from the given byte array
	public static void playSound(byte [] sound) {
		if(Gui.sound) {
			try {
	            Sequencer sequencer = MidiSystem.getSequencer(); // Get the default Sequencer
	            if (sequencer==null) {
	                System.err.println("Sequencer device not supported");
	                return;
	            } 
	            sequencer.open(); // Open device
	            Sequence sequence = MidiSystem.getSequence(new ByteArrayInputStream(sound));
	            sequencer.setSequence(sequence); // load it into sequencer
	            sequencer.start();  // start the playback
	        } catch (MidiUnavailableException ex) {
	            ex.printStackTrace();
	        } catch (InvalidMidiDataException ex) {
	            ex.printStackTrace();
	        } catch (IOException ex) {
	            ex.printStackTrace();
	        }  
		}
	}
}