import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.Collections;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.time.Period;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;

public class Challenge {
    
    enum SubscriptionType {
        ONEOFF, DAILY, MONTHLY, YEARLY
    }
    
    // Path to the transactions CSV file.
    static String transactionsFilePathString = "./transactions.csv";
    
    // The first and last year of the transactions' span.
    static final int startYear = 1966;
    static final int endYear = 2014;
    
    public static void main(String[] args) {
        
        if (args.length == 1)
            transactionsFilePathString = args[0];
        
        /* 
         * This Map holds every subscription ID along with each transaction date for a given subscription.
         * If you want speed, use a HashMap. If you want your subscriptions to be sorted, use a TreeMap.
         * The challenge doesn't specify whether the subscriptions should be ordered by number, so use a HashMap for now.
         */
        Map<Integer, List<LocalDate>> subscriptions = new HashMap<Integer, List<LocalDate>>();
        
        // Maps each subscription type to a list of all subscriptions of that type.
        Map<SubscriptionType, List<Integer>> subscriptionTypes = new HashMap<SubscriptionType, List<Integer>>();
        for (SubscriptionType type : SubscriptionType.values())
            subscriptionTypes.put(type, new ArrayList<Integer>());
        
        /*
         * Maps each subscription ID with the price of one transaction from their subscription.
         * It was observed that for a given subscription, each transaction is the same price.
         */
        Map<Integer, Double> subscriptionTransactionPrices = new HashMap<Integer, Double>();
        
        // Each index represents a year, and each element is the total revenue for that year.
        int[] revenues = new int[(endYear - startYear) + 1];
        
        // Each index represents a year, and each element is the difference in revenue between that year and the previous year.
        int[] revenueDiffs = new int[(endYear - startYear) + 1];
        
        // Formatter for currency/revenue values.
        NumberFormat revenueFormatter = NumberFormat.getCurrencyInstance();
        
        // Formatter for date values.
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("L/d/u");
        
        // Read the data from the CSV file.
        Path transactionsFilePath = Paths.get(transactionsFilePathString);
        List<String> allLines;
        try {
            allLines = Files.readAllLines(transactionsFilePath, StandardCharsets.US_ASCII);
        }
        catch (IOException exc) {
            System.err.println("Could not find input file \"" + transactionsFilePathString + "\"");
            return;
        }
        
        // Iterate through each transaction line, populating the revenue and subscription data structures.
        for (String transactionLine : allLines) {
            if (transactionLine.startsWith("Id"))
                continue;
            
            String[] components = transactionLine.split(","); // Format is id,subscriptionId,amount,date
            int subscriptionId = Integer.parseInt(components[1]);
            double amount = Double.parseDouble(components[2]);
            LocalDate date = LocalDate.parse(components[3], dateFormatter);
            
            revenues[date.getYear() - startYear] += amount;
            
            subscriptions.putIfAbsent(subscriptionId, new ArrayList<LocalDate>());
            // Because the transactions are sorted in the input file by date, the dates will be nicely sorted in each transaction's date list.
            subscriptions.get(subscriptionId).add(date);
            
            subscriptionTransactionPrices.putIfAbsent(subscriptionId, amount);
        }
        
        System.out.println("Subscription ID, type (one-off, daily, monthly, yearly), and duration:");
        // Determine and print the type and duration of each subscription.
        for (Integer subscriptionId : subscriptions.keySet()) {
            List<LocalDate> transactionDates = subscriptions.get(subscriptionId);
            
            SubscriptionType type = null;
            String duration = "";
            int numTransactionDates = transactionDates.size();
            
            if (numTransactionDates == 1) {
                type = SubscriptionType.ONEOFF;
                duration = "1 day";
            }
            else {
                LocalDate first = transactionDates.get(0);
                LocalDate second = transactionDates.get(1);
                
                if (second.getDayOfMonth() != first.getDayOfMonth()) {
                    type = SubscriptionType.DAILY;
                    duration = numTransactionDates + " days";
                }
                else if (second.getMonthValue() != first.getMonthValue()) {
                    type = SubscriptionType.MONTHLY;
                    // Split any months >= 12 into years.
                    if (numTransactionDates >= 12)
                        duration = (numTransactionDates / 12)  + " year" + (numTransactionDates/12 == 1 ? "" : "s");
                    // Add the remaining months.
                    if (numTransactionDates % 12 != 0)
                        duration += (numTransactionDates >= 12 ? " and " : "") + (numTransactionDates % 12) + " month" + (numTransactionDates % 12 == 1 ? "" : "s");
                    // If we converted to years, we should also give the total amount of months.
                    if (numTransactionDates >= 12)
                        duration += ", or " + numTransactionDates + " months";
                }
                else if (second.getYear() != first.getYear()) {
                    type = SubscriptionType.YEARLY;
                    duration = numTransactionDates + " years";
                }
                
            }
            if (type != SubscriptionType.ONEOFF)
                subscriptionTypes.get(type).add(subscriptionId);
            System.out.println(subscriptionId + " " + type.toString() + ":\t" + duration);
        }

        System.out.println("\nYearly revenue:");
        // Print the revenue for each year and calculate the revenue differences for each year.
        for (int yearIndex = 0; yearIndex <= (endYear - startYear); ++yearIndex) {
            System.out.println((yearIndex + startYear) + ": " + revenueFormatter.format(revenues[yearIndex]));
            revenueDiffs[yearIndex] = revenues[yearIndex] - (yearIndex == 0 ? 0 : revenues[yearIndex - 1]);
        }
        
        // Get the years with highest growth and years with highest loss.
        int numYears = 10;
        ArrayList<Integer> highestGrowthYears = new ArrayList<Integer>(numYears);
        ArrayList<Integer> highestLossYears = new ArrayList<Integer>(numYears);
        while (highestGrowthYears.size() < numYears) {
            int highestGrowth = 0;
            int yearIndexOfHighestGrowth = 0;
            int highestLoss = 0;
            int yearIndexOfHighestLoss = 0;
            for (int yearIndex = 0; yearIndex <= (endYear - startYear); ++yearIndex) {
                if (revenueDiffs[yearIndex] > highestGrowth && !highestGrowthYears.contains(yearIndex + startYear)) {
                    highestGrowth = revenueDiffs[yearIndex];
                    yearIndexOfHighestGrowth = yearIndex;
                }
                else if (revenueDiffs[yearIndex] < highestLoss && !highestLossYears.contains(yearIndex + startYear)) {
                    highestLoss = revenueDiffs[yearIndex];
                    yearIndexOfHighestLoss = yearIndex;
                }
            }
            highestGrowthYears.add(yearIndexOfHighestGrowth + startYear);
            highestLossYears.add(yearIndexOfHighestLoss + startYear);
        }

        System.out.println("\nYearly growth/loss (parentheses indicate a loss):");
        for (int yearIndex = 0; yearIndex <= (endYear - startYear); ++yearIndex)
            System.out.println((yearIndex + startYear) + ": " + revenueFormatter.format(revenueDiffs[yearIndex]));
        
        System.out.println("\n" + numYears + " years with highest growth:");
        for (Integer year : highestGrowthYears)
            System.out.println(year + ": " + revenueFormatter.format(revenueDiffs[year - startYear]));
        
        System.out.println("\n" + numYears + " years with highest loss:");
        for (Integer year : highestLossYears)
            System.out.println(year + ": " + revenueFormatter.format(revenueDiffs[year - startYear]));
        
        SubscriptionType[] subscriptionTypeValues = { SubscriptionType.DAILY, SubscriptionType.MONTHLY, SubscriptionType.YEARLY };
        Map<SubscriptionType, Double> averageTypeDurations = new HashMap<SubscriptionType, Double>(subscriptionTypeValues.length);
        LocalDate firstDayOfNextYear = LocalDate.of(endYear + 1, 1, 1);
        double expectedRevenueFromReturningSubscriptions = 0;
        double expectedRevenueFromNewSubscriptions = 0;
        int transactionsPerYear = 0;
        
        /*
         * Predict revenue from returning subscribers for the next year. This is done by finding the rate at which subscribers are not returning.
         * For example, we can find:
         * int a = How many people did not continue their subscription in 2013?
         * int b = How many people did not continue their subscription in 2014?
         * b - (b - a > 0 ? (b - a) : 0) = How many people will likely not continue their subscription in 2015?
         *
         * We can then calculate how much yearly revenue will be obtained from the subscribers who will likely return in 2015.
         *
         * Next, we will want to find how much revenue to expect from new subscribers.
         * Organizing the data appropriately and inspecting it shows that this company hasn't had a new subscriber for almost 20 years,
         * so we shouldn't expect any revenue from new subscribers.
         * However, we can still calculate this in a similar manner to what we did above (in the event that we were actually expecting some new subscribers).
         * int a = How many new subscribers were there in 2013?
         * int b = How many new subscribers were there in 2014?
         * b - (b - a > 0 ? (b - a) : 0) = Approximately how many new subscribers can we expect for 2015?
         *
         * We don't know what the transaction price for our new subscribers will be, so we take the average of previous transaction prices.
         * We can then calculate how much revenue to expect from new subscribers.
         */
        for (SubscriptionType type : subscriptionTypeValues) {
            
            if (type == SubscriptionType.DAILY)
                transactionsPerYear = 365;
            else if (type == SubscriptionType.MONTHLY)
                transactionsPerYear = 12;
            else if (type == SubscriptionType.YEARLY)
                transactionsPerYear = 1;
            
            int numSubscriptionsDidNotReturnLastYear = 0, numSubscriptionsDidNotReturnThisYear = 0, numSubscriptionsWillReturnNextYear = 0;
            int numNewSubscriptionsLastYear = 0, numNewSubscriptionsThisYear = 0, numNewSubscriptionsNextYear = 0;
            List<Integer> currentSubscriptions = new ArrayList<Integer>();
            double totalTransactionPrice = 0;
            int totalSubscriptions = 0;
            
            for (int subscriptionId : subscriptionTypes.get(type)) {
                List<LocalDate> transactions = subscriptions.get(subscriptionId);
                LocalDate firstTransaction = transactions.get(0);
                LocalDate lastTransaction = transactions.get(transactions.size() - 1);
                
                long yearOfFirstTransaction = firstTransaction.getYear();
                if (yearOfFirstTransaction == (endYear - 1))
                    numNewSubscriptionsLastYear++;
                else if (yearOfFirstTransaction == endYear)
                    numNewSubscriptionsThisYear++;
                
                long yearOfLastTransaction = lastTransaction.getYear();
                if (yearOfLastTransaction == (endYear - 2))
                    numSubscriptionsDidNotReturnLastYear++;
                else if (yearOfLastTransaction == endYear - 1)
                    numSubscriptionsDidNotReturnThisYear++;
                else if (yearOfLastTransaction == endYear)
                    currentSubscriptions.add(subscriptionId);
                
                totalTransactionPrice += subscriptionTransactionPrices.get(subscriptionId);
                ++totalSubscriptions;
            }
            int numCurrentSubscriptions = currentSubscriptions.size();
            int delta = numSubscriptionsDidNotReturnThisYear - numSubscriptionsDidNotReturnLastYear;
            numSubscriptionsWillReturnNextYear = delta < 0 ? numCurrentSubscriptions + delta : numCurrentSubscriptions;
            
            //We can't predict who won't be returning, so remove subscriptions from the beginning of the current subscription list.
            currentSubscriptions.subList(0, numCurrentSubscriptions - numSubscriptionsWillReturnNextYear).clear();
            
            // Calculate how much yearly revenue will be obtained from the remaining returning subscriptions.
            for (int subscriptionId : currentSubscriptions) {
                double subscriptionTransactionPrice = subscriptionTransactionPrices.get(subscriptionId);
                double totalYearlyRevenueFromSubscription = subscriptionTransactionPrice * transactionsPerYear;
                expectedRevenueFromReturningSubscriptions += totalYearlyRevenueFromSubscription;
            }

            // Calculate how much revenue we'll be expecting from new subscribers.
            delta = numNewSubscriptionsThisYear - numNewSubscriptionsLastYear;
            numNewSubscriptionsNextYear = numNewSubscriptionsThisYear + delta;
            numNewSubscriptionsNextYear = numNewSubscriptionsNextYear > 0 ? numNewSubscriptionsNextYear : 0;
            
            double averageTransactionPrice = totalTransactionPrice / (double) totalSubscriptions;
            expectedRevenueFromNewSubscriptions += averageTransactionPrice * transactionsPerYear * numNewSubscriptionsNextYear;
        }
        System.out.println("\nExpected total revenue for " + (endYear + 1) + " is " + revenueFormatter.format(expectedRevenueFromNewSubscriptions + expectedRevenueFromReturningSubscriptions));
    }
}

