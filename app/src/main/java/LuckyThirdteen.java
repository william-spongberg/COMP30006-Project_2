// LuckyThirteen.java

import ch.aplu.jcardgame.*;
import ch.aplu.jgamegrid.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import _card.Rank;
import _card.Suit;
import _player.Player;
import _player.PlayerFactory;

@SuppressWarnings("serial")
public class LuckyThirdteen extends CardGame {

    // TODO: move values put here to static finals in info expert?
    // TODO: move attributes into respective classes?
    final String trumpImage[] = { "bigspade.gif", "bigheart.gif", "bigdiamond.gif", "bigclub.gif" };
    static public final int seed = 30008;
    static final Random random = new Random(seed);
    private Properties properties;
    private StringBuilder logResult = new StringBuilder();
    private List<List<String>> playerAutoMovements = new ArrayList<>();

    public boolean rankGreater(Card card1, Card card2) {
        // Warning: Reverse rank order of cards (see comment on enum)
        // FIXME: why warning?
        return card1.getRankId() < card2.getRankId();
    }

    // TODO: increment version per major commit?
    private final String version = "1.0";

    public int nbPlayers = 4;
    public final int nbStartCards = 2;
    public final int nbFaceUpCards = 2;

    private final int handWidth = 400;
    private final int trickWidth = 40;

    private static final int THIRTEEN_GOAL = 13;

    private final Deck deck = new Deck(Suit.values(), Rank.values(), "cover");

    private final Location[] handLocations = {
            new Location(350, 625),
            new Location(75, 350),
            new Location(350, 75),
            new Location(625, 350)
    };
    private final Location[] scoreLocations = {
            new Location(575, 675),
            new Location(25, 575),
            new Location(575, 25),
            new Location(575, 575)
    };

    private Actor[] scoreActors = { null, null, null, null };

    private final Location trickLocation = new Location(350, 350);
    private final Location textLocation = new Location(350, 450);

    private int thinkingTime = 2000;
    private int delayTime = 600;

    private Hand[] hands;

    public void setStatus(String string) {
        setStatusText(string);
    }

    private int[] scores = new int[nbPlayers];

    private int[] autoIndexHands = new int[nbPlayers];
    private boolean isAuto = false;

    private Hand playingArea;
    private Hand pack;

    Font bigFont = new Font("Arial", Font.BOLD, 36);

    private Card selected;

    // TODO: move scoring to new score class?
    private void initScore() {
        for (int i = 0; i < nbPlayers; i++) {
            String text = "[" + String.valueOf(scores[i]) + "]";
            scoreActors[i] = new TextActor(text, Color.WHITE, bgColor, bigFont);
            addActor(scoreActors[i], scoreLocations[i]);
        }
    }

    // private Actor[] initScore(int nbPlayers, int[] scores, Color bgColor, Font
    // bigFont) {
    // for (int i = 0; i < nbPlayers; i++) {
    // String text = "[" + String.valueOf(scores[i]) + "]";
    // scoreActors[i] = new TextActor(text, Color.WHITE, bgColor, bigFont);
    // //addActor(scoreActors[i], scoreLocations[i]);
    // }
    // return scoreActors;
    // }

    private int getScorePrivateCard(Card card) {
        Rank rank = (Rank) card.getRank();
        Suit suit = (Suit) card.getSuit();

        return rank.getScoreCardValue() * suit.getMultiplicationFactor();
    }

    private int getScorePublicCard(Card card) {
        Rank rank = (Rank) card.getRank();
        return rank.getScoreCardValue() * Suit.PUBLIC_CARD_MULTIPLICATION_FACTOR;
    }

    private int calculateMaxScoreForThirteenPlayer(int playerIndex) {// , Hand[] hands, Hand playingArea) {
        List<Card> privateCards = hands[playerIndex].getCardList();
        List<Card> publicCards = playingArea.getCardList();
        Card privateCard1 = privateCards.get(0);
        Card privateCard2 = privateCards.get(1);
        Card publicCard1 = publicCards.get(0);
        Card publicCard2 = publicCards.get(1);

        int maxScore = 0;

        // TODO: refactor to use a list of cards instead of multiple if statements
        if (isThirteenCards(privateCard1, privateCard2)) {
            int score = getScorePrivateCard(privateCard1) + getScorePrivateCard(privateCard2);
            if (maxScore < score) {
                maxScore = score;
            }
        }

        if (isThirteenCards(privateCard1, publicCard1)) {
            int score = getScorePrivateCard(privateCard1) + getScorePublicCard(publicCard1);
            if (maxScore < score) {
                maxScore = score;
            }
        }

        if (isThirteenCards(privateCard1, publicCard2)) {
            int score = getScorePrivateCard(privateCard1) + getScorePublicCard(publicCard2);
            if (maxScore < score) {
                maxScore = score;
            }
        }

        if (isThirteenCards(privateCard2, publicCard1)) {
            int score = getScorePrivateCard(privateCard2) + getScorePublicCard(publicCard1);
            if (maxScore < score) {
                maxScore = score;
            }
        }

        if (isThirteenCards(privateCard2, publicCard2)) {
            int score = getScorePrivateCard(privateCard2) + getScorePublicCard(publicCard2);
            if (maxScore < score) {
                maxScore = score;
            }
        }

        return maxScore;
    }

    private void calculateScoreEndOfRound() {
        List<Boolean> isThirteenChecks = Arrays.asList(false, false, false, false);
        for (int i = 0; i < hands.length; i++) {
            isThirteenChecks.set(i, isThirteen(i));
        }
        List<Integer> indexesWithThirteen = new ArrayList<>();
        for (int i = 0; i < isThirteenChecks.size(); i++) {
            if (isThirteenChecks.get(i)) {
                indexesWithThirteen.add(i);
            }
        }
        long countTrue = indexesWithThirteen.size();
        Arrays.fill(scores, 0);
        if (countTrue == 1) {
            int winnerIndex = indexesWithThirteen.get(0);
            scores[winnerIndex] = 100;
        } else if (countTrue > 1) {
            for (Integer thirteenIndex : indexesWithThirteen) {
                scores[thirteenIndex] = calculateMaxScoreForThirteenPlayer(thirteenIndex);
            }

        } else {
            for (int i = 0; i < scores.length; i++) {
                scores[i] = getScorePrivateCard(hands[i].getCardList().get(0)) +
                        getScorePrivateCard(hands[i].getCardList().get(1));
            }
        }
    }

    private void updateScore(int player) {
        // FIXME: why create new actor each time? just change text
        removeActor(scoreActors[player]);
        int displayScore = Math.max(scores[player], 0);
        String text = "P" + player + "[" + String.valueOf(displayScore) + "]";
        scoreActors[player] = new TextActor(text, Color.WHITE, bgColor, bigFont);
        addActor(scoreActors[player], scoreLocations[player]);
    }

    private void initScores() {
        Arrays.fill(scores, 0);
    }

    // TODO: move into new getter classes?
    public static <T extends Enum<?>> T randomEnum(Class<T> clazz) {
        // FIXME: method never used
        int x = random.nextInt(clazz.getEnumConstants().length);
        return clazz.getEnumConstants()[x];
    }

    // return random Card from ArrayList
    public static Card randomCard(ArrayList<Card> list) {
        int x = random.nextInt(list.size());
        return list.get(x);
    }

    public Card getRandomCard(Hand hand) {
        dealACardToHand(hand);

        delay(thinkingTime);

        int x = random.nextInt(hand.getCardList().size());
        return hand.getCardList().get(x);
    }

    // TODO: combine getRankFromString and getSuitFromString into one method?
    private Rank getRankFromString(String cardName) {
        String rankString = cardName.substring(0, cardName.length() - 1);
        Integer rankValue = Integer.parseInt(rankString);

        for (Rank rank : Rank.values()) {
            if (rank.getRankCardValue() == rankValue) {
                return rank;
            }
        }

        return Rank.ACE;
    }

    private Suit getSuitFromString(String cardName) {
        String rankString = cardName.substring(0, cardName.length() - 1);
        // FIXME: rankString is not used
        String suitString = cardName.substring(cardName.length() - 1, cardName.length());
        // FIXME: rankValue is not used
        Integer rankValue = Integer.parseInt(rankString);

        for (Suit suit : Suit.values()) {
            if (suit.getSuitShortHand().equals(suitString)) {
                return suit;
            }
        }
        return Suit.CLUBS;
    }

    private Card getCardFromList(List<Card> cards, String cardName) {
        Rank cardRank = getRankFromString(cardName);
        Suit cardSuit = getSuitFromString(cardName);
        for (Card card : cards) {
            if (card.getSuit() == cardSuit
                    && card.getRank() == cardRank) {
                return card;
            }
        }

        return null;
    }

    // TODO: move game referee methods into seperate class?
    private boolean isThirteenFromPossibleValues(int[] possibleValues1, int[] possibleValues2) {
        for (int value1 : possibleValues1) {
            for (int value2 : possibleValues2) {
                if (value1 + value2 == THIRTEEN_GOAL) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isThirteenCards(Card card1, Card card2) {
        Rank rank1 = (Rank) card1.getRank();
        Rank rank2 = (Rank) card2.getRank();
        return isThirteenFromPossibleValues(rank1.getPossibleSumValues(), rank2.getPossibleSumValues());
    }

    private boolean isThirteenMixedCards(List<Card> privateCards, List<Card> publicCards) {
        for (Card privateCard : privateCards) {
            for (Card publicCard : publicCards) {
                if (isThirteenCards(privateCard, publicCard)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isThirteen(int playerIndex) {
        List<Card> privateCards = hands[playerIndex].getCardList();
        List<Card> publicCards = playingArea.getCardList();
        boolean isThirteenPrivate = isThirteenCards(privateCards.get(0), privateCards.get(1));
        boolean isThirteenMixed = isThirteenMixedCards(privateCards, publicCards);
        return isThirteenMixed || isThirteenPrivate;
    }

    // TODO: move into new dealer class?
    private void dealingOut(Hand[] hands, int nbPlayers, int nbCardsPerPlayer, int nbSharedCards) {
        pack = deck.toHand(false);

        String initialShareKey = "shared.initialcards";
        String initialShareValue = properties.getProperty(initialShareKey);
        if (initialShareValue != null) {
            String[] initialCards = initialShareValue.split(",");
            for (String initialCard : initialCards) {
                if (initialCard.length() <= 1) {
                    continue;
                }
                Card card = getCardFromList(pack.getCardList(), initialCard);
                if (card != null) {
                    card.removeFromHand(true);
                    playingArea.insert(card, true);
                }
            }
        }
        int cardsToShare = nbSharedCards - playingArea.getNumberOfCards();

        for (int j = 0; j < cardsToShare; j++) {
            if (pack.isEmpty())
                return;
            Card dealt = randomCard(pack.getCardList());
            dealt.removeFromHand(true);
            playingArea.insert(dealt, true);
        }

        for (int i = 0; i < nbPlayers; i++) {
            String initialCardsKey = "players." + i + ".initialcards";
            String initialCardsValue = properties.getProperty(initialCardsKey);
            if (initialCardsValue == null) {
                continue;
            }
            String[] initialCards = initialCardsValue.split(",");
            for (String initialCard : initialCards) {
                if (initialCard.length() <= 1) {
                    continue;
                }
                Card card = getCardFromList(pack.getCardList(), initialCard);
                if (card != null) {
                    card.removeFromHand(false);
                    hands[i].insert(card, false);
                }
            }
        }

        for (int i = 0; i < nbPlayers; i++) {
            int cardsToDealt = nbCardsPerPlayer - hands[i].getNumberOfCards();
            for (int j = 0; j < cardsToDealt; j++) {
                if (pack.isEmpty())
                    return;
                Card dealt = randomCard(pack.getCardList());
                dealt.removeFromHand(false);
                hands[i].insert(dealt, false);
            }
        }
    }

    private void dealACardToHand(Hand hand) {
        if (pack.isEmpty())
            return;
        Card dealt = randomCard(pack.getCardList());
        dealt.removeFromHand(false);
        hand.insert(dealt, true);
    }

    // TODO: move to new log class?
    private void addCardPlayedToLog(int player, List<Card> cards) {
        if (cards.size() < 2) {
            return;
        }
        logResult.append("P" + player + "-");

        for (int i = 0; i < cards.size(); i++) {
            Rank cardRank = (Rank) cards.get(i).getRank();
            Suit cardSuit = (Suit) cards.get(i).getSuit();
            logResult.append(cardRank.getRankCardLog() + cardSuit.getSuitShortHand());
            if (i < cards.size() - 1) {
                logResult.append("-");
            }
        }
        logResult.append(",");
    }

    private void addRoundInfoToLog(int roundNumber) {
        logResult.append("Round" + roundNumber + ":");
    }

    private void addEndOfRoundToLog() {
        logResult.append("Score:");
        for (int i = 0; i < scores.length; i++) {
            logResult.append(scores[i] + ",");
        }
        logResult.append("\n");
    }

    private void addEndOfGameToLog(List<Integer> winners) {
        logResult.append("EndGame:");
        for (int i = 0; i < scores.length; i++) {
            logResult.append(scores[i] + ",");
        }
        logResult.append("\n");
        logResult.append(
                "Winners:" + String.join(", ", winners.stream().map(String::valueOf).collect(Collectors.toList())));
    }

    // TODO: move to new auto move class?
    private Card applyAutoMovement(Hand hand, String nextMovement) {
        if (pack.isEmpty())
            return null;
        String[] cardStrings = nextMovement.split("-");
        String cardDealtString = cardStrings[0];
        Card dealt = getCardFromList(pack.getCardList(), cardDealtString);
        if (dealt != null) {
            dealt.removeFromHand(false);
            hand.insert(dealt, true);
        } else {
            System.out.println("cannot draw card: " + cardDealtString + " - hand: " + hand);
        }

        if (cardStrings.length > 1) {
            String cardDiscardString = cardStrings[1];
            return getCardFromList(hand.getCardList(), cardDiscardString);
        } else {
            return null;
        }
    }

    private void setupPlayerAutoMovements() {
        String player0AutoMovement = properties.getProperty("players.0.cardsPlayed");
        String player1AutoMovement = properties.getProperty("players.1.cardsPlayed");
        String player2AutoMovement = properties.getProperty("players.2.cardsPlayed");
        String player3AutoMovement = properties.getProperty("players.3.cardsPlayed");

        // FIXME: playerMovements should be immediately initialised with properties,
        // remove if statements
        String[] playerMovements = new String[] { "", "", "", "" };

        if (player0AutoMovement != null) {
            playerMovements[0] = player0AutoMovement;
        }

        if (player1AutoMovement != null) {
            playerMovements[1] = player1AutoMovement;
        }

        if (player2AutoMovement != null) {
            playerMovements[2] = player2AutoMovement;
        }

        if (player3AutoMovement != null) {
            playerMovements[3] = player3AutoMovement;
        }

        for (int i = 0; i < playerMovements.length; i++) {
            // FIXME: unnecessary to define here
            String movementString = playerMovements[i];
            if (movementString.equals("")) {
                playerAutoMovements.add(new ArrayList<>());
                continue;
            }
            List<String> movements = Arrays.asList(movementString.split(","));
            playerAutoMovements.add(movements);
        }
    }

    // TODO: move variables to more appopriate spots
    Player[] players;
    List<String> playerTypes;
    List<String> initPlayerHands;
    String initSharedCards;

    //Dealer dealer; // necessary??

    private void initGame() {
        // FIXME: each player should contain hand and score

        // read properties file
        PropertiesReader pReader = new PropertiesReader(properties);
        nbPlayers = pReader.getNumPlayers();
        isAuto = pReader.isAuto();
        thinkingTime = pReader.getThinkingTime();
        delayTime = pReader.getDelayTime();
        playerTypes = pReader.getPlayerTypes();
        playerAutoMovements = pReader.getPlayerAutoMovements();
        initPlayerHands = pReader.getInitialPlayerHands();
        initSharedCards = pReader.getInitialSharedCards();

        // create players
        players = new Player[nbPlayers];
        PlayerFactory pFactory = new PlayerFactory();
        for (int i = 0; i < nbPlayers; i++) {
            players[i] = pFactory.createPlayer(playerTypes.get(i), initPlayerHands.get(i), initSharedCards, isAuto,
                    playerAutoMovements.get(i));
        }

        // deal out cards to players
        hands = new Hand[nbPlayers];
        for (int i = 0; i < nbPlayers; i++) {
            hands[i] = new Hand(deck);
            hands[i].sort(Hand.SortType.SUITPRIORITY, false);
            players[i].setHand(hands[i]);
        }
        //dealingOut(hands, nbPlayers, nbStartCards, nbFaceUpCards);

        

        // Set up human player for interaction
        // FIXME: move into Human class as child of Player class
        CardListener cardListener = new CardAdapter() // Human Player plays card
        {
            @Override
            public void leftDoubleClicked(Card card) {
                selected = card;
                hands[0].setTouchEnabled(false);
            }
        };

        if (!isAuto) {
            //hands[0].addCardListener(cardListener);
            for (Player player : players) {
                if (player instanceof Human) {
                    Human humanPlayer = (Human) player;
                    //humanPlayer.setController(new ManualController());
                    // TODO: fix this, should be in controller
                    humanPlayer.addCardListener(cardListener);
                }
            }
        }

        // UI stuff
        playingArea = new Hand(deck);
        playingArea.setView(this, new RowLayout(trickLocation, (playingArea.getNumberOfCards() + 2) * trickWidth));
        playingArea.draw();
        RowLayout[] layouts = new RowLayout[nbPlayers];
        for (int i = 0; i < nbPlayers; i++) {
            layouts[i] = new RowLayout(handLocations[i], handWidth);
            layouts[i].setRotationAngle(90 * i);
            hands[i].setView(this, layouts[i]);
            hands[i].setTargetArea(new TargetArea(trickLocation));
            hands[i].draw();
        }
    }

    private void playGame() {
        // initialize winner + round number
        // FIXME: winner not used
        int winner = 0;
        int roundNumber = 1;

        // update the score for each player
        // FIXME: necessary to update score here? should already be initialised
        for (int i = 0; i < nbPlayers; i++)
            updateScore(i);

        // initialize list of cards played
        List<Card> cardsPlayed = new ArrayList<>();

        // log initial round number
        addRoundInfoToLog(roundNumber);

        // initialize next player
        int nextPlayer = 0;

        // start game loop
        // FIXME: 4 should be MAX_ROUNDS
        while (roundNumber <= 4) {
            selected = null;
            boolean finishedAuto = false;

            // if game is set to auto
            if (isAuto) {
                // get next player's auto index and movements
                int nextPlayerAutoIndex = autoIndexHands[nextPlayer];
                List<String> nextPlayerMovement = playerAutoMovements.get(nextPlayer);
                String nextMovement = "";

                // if there are more movements
                if (nextPlayerMovement.size() > nextPlayerAutoIndex) {
                    // get next movement and increment the auto index
                    nextMovement = nextPlayerMovement.get(nextPlayerAutoIndex);
                    nextPlayerAutoIndex++;

                    // update the auto index for the player
                    autoIndexHands[nextPlayer] = nextPlayerAutoIndex;
                    Hand nextHand = hands[nextPlayer];

                    // apply player movement
                    selected = applyAutoMovement(nextHand, nextMovement);
                    delay(delayTime);

                    // if card was selected, remove from hand
                    if (selected != null) {
                        selected.removeFromHand(true);
                    } else {
                        // if no card was selected, get random card and remove from hand
                        // (default behaviour)
                        // TODO: move random card selection to Bot Random class
                        selected = getRandomCard(hands[nextPlayer]);
                        selected.removeFromHand(true);
                    }
                } else {
                    // if no more movements for player, set finishedAuto to true
                    finishedAuto = true;
                }
            }

            // if game is not set to auto or if finishedAuto is true
            if (!isAuto || finishedAuto) {
                // if the next player is player 0
                if (0 == nextPlayer) {
                    // enable touch for player 0
                    hands[0].setTouchEnabled(true);

                    // set the status message and deal a card to player 0
                    setStatus("Player 0 is playing. Please double click on a card to discard");
                    selected = null;
                    dealACardToHand(hands[0]);

                    // wait until a card is selected
                    while (null == selected) {
                        // FIXME: delay here is not necessary
                        delay(delayTime);
                    }

                    // remove selected card from the hand
                    selected.removeFromHand(true);
                } else {
                    // if the next player is not player 0 (human), set the status message
                    setStatusText("Player " + nextPlayer + " thinking...");

                    // get random card and remove it from the hand
                    // FIXME: doing random bot behaviour here, means if human is playing all bots
                    // will be random
                    selected = getRandomCard(hands[nextPlayer]);
                    selected.removeFromHand(true);
                }
            }

            // log cards played by the player
            addCardPlayedToLog(nextPlayer, hands[nextPlayer].getCardList());

            // if card was selected
            if (selected != null) {
                // add card to the list of cards played
                cardsPlayed.add(selected);
                // set face up
                selected.setVerso(false);
                delay(delayTime);
            }

            // next player's turn
            nextPlayer = (nextPlayer + 1) % nbPlayers;

            // if the next player is player 0, increment the round number and log the end of
            // round scores
            if (nextPlayer == 0) {
                roundNumber++;
                addEndOfRoundToLog();

                // if more rounds, log the round information
                // FIXME: 4 should be MAX_ROUNDS
                if (roundNumber <= 4) {
                    addRoundInfoToLog(roundNumber);
                }
            }

            // if game is over, calculate final score
            // FIXME: 4 should be MAX_ROUNDS
            if (roundNumber > 4) {
                calculateScoreEndOfRound();
            }

            // delay before next round
            delay(delayTime);
        }
    }

    public String runApp() {
        setTitle("LuckyThirteen (V" + version + ") Constructed for UofM SWEN30006 with JGameGrid (www.aplu.ch)");
        setStatusText("Initialising...");

        // FIXME: create Score object here - or should it be inside the game?
        initScores();
        initScore();

        // Actor[] scoreActors = initScore(nbPlayers, scores, bgColor, bigFont);

        // for (int i = 0; i < nbPlayers; i++)
        // addActor(scoreActors[i], scoreLocations[i]);

        setupPlayerAutoMovements();

        initGame();
        playGame();

        for (int i = 0; i < nbPlayers; i++)
            updateScore(i);
        int maxScore = 0;
        for (int i = 0; i < nbPlayers; i++)
            if (scores[i] > maxScore)
                maxScore = scores[i];
        final List<Integer> winners = new ArrayList<>();
        for (int i = 0; i < nbPlayers; i++)
            if (scores[i] == maxScore)
                winners.add(i);
        String winText;
        if (winners.size() == 1) {
            winText = "Game over. Winner is player: " +
                    winners.iterator().next();
        } else {
            winText = "Game Over. Drawn winners are players: " +
                    String.join(", ", winners.stream().map(String::valueOf).collect(Collectors.toList()));
        }
        addActor(new Actor("sprites/gameover.gif"), textLocation);
        setStatusText(winText);
        refresh();
        addEndOfGameToLog(winners);

        return logResult.toString();
    }

    public LuckyThirdteen(Properties properties) {
        super(700, 700, 30);
        this.properties = properties;
        isAuto = Boolean.parseBoolean(properties.getProperty("isAuto"));
        thinkingTime = Integer.parseInt(properties.getProperty("thinkingTime", "200"));
        delayTime = Integer.parseInt(properties.getProperty("delayTime", "50"));
    }

}
