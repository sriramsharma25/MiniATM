package edu.project.atmdesign;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * 
 * @author Sriram Neelakandha Sharma
 *
 * The following are inferred and/or assumed based on the question and the examples given in the project document
 * 	1. The entire project is supposed to be delivered as a single class. 
 * 		This, in itself, will be a huge limitation in delivering with a good project structure like Constants, Exception, Junits, Properties, etc
 * 	2. A space is assumed after every comma and after every colon symbols in the deposit part of the entry
 * 		We are not testing for the edgecase of availability of space or colon or comma as they are assumed to be available as a valid entry 
 *	3. Deposit keyword is assumed to be available for the input entry else it is taken as withdrawal by default
 *	4. Deposit Number is not validated... eg: Note that Deposit No 2 is done first and then Deposit No 1... So, No order validation
 *		Eg: 	Deposit 2: 20s: 3, 5s: 18, 1s: 4
 *				Balance: 20s=3, 10s=0, 5s=18, 1s=4, Total=154.0
 *				Deposit 1: 10s: 8, 5s: 20
 *				Balance: 20s=3, 10s=8, 5s=38, 1s=4, Total=334.0
 *	5. Float is used for output as we could expand with smaller denomination features like 0.50 (50 cents) or 0.25 (25 cents) in future
 *	6. Currently floating point is supported for Deposits only (Not Withdrawals) as a welcome feature. This can be extended to Withdrawals with few changes
 *
 */
public class ATM {
	
	static boolean RESULT_DISPLAY_AS_INTEGER = true;
	//Below list can be used instead of sorting the array, but we have to know the denominations earlier to do this way
	//We are using this list for withdrawal purpose and sorting technique for deposit purpose!
	static List<String> DENOMINATIONS_USED = Arrays.asList(new String[] {"20","10","5","1"});
	
	public ATM() {
		
	}
	
	public static void main(String[] args) {
		ATM atm = new ATM();
		Map<String, String> allDenominationsMap = atm.initializeAllDenominationsMap(DENOMINATIONS_USED);
		boolean deposit = false;
		while(true) {
			Scanner scanner = new Scanner(System.in);
			try {
				if(scanner.next().equalsIgnoreCase("Deposit")) {
					deposit = true;
				} else {
					deposit = false;
				}
				scanner.next();
				String line = scanner.nextLine();
				StringTokenizer st = new StringTokenizer(line, " ");
				Map<String, String> denominationsMap = new HashMap<>();
				String withdrawalAmount = null;
				while(st.hasMoreTokens()) {
					if(deposit) {
						String denomination = st.nextToken();
						String value = st.nextToken();
						denominationsMap.put(denomination.split("s:")[0], value.split(",")[0]);
					} else {
						withdrawalAmount = st.nextToken();
					}
				}
				if(deposit && atm.depositRules(denominationsMap)) {
					atm.deposit(denominationsMap, allDenominationsMap);	
					atm.displayBalanceWithEntries(allDenominationsMap);
				} else if(atm.withdrawalRules(withdrawalAmount, allDenominationsMap)) {
					List<Integer> availableDenominationsList = atm.getAvailableDenominations(allDenominationsMap);
					Map<String, String> withdrawalMap = atm.withdrawal(withdrawalAmount, allDenominationsMap, availableDenominationsList);
					if(withdrawalMap!=null) {
						atm.updateAllDenominationsMap(withdrawalMap, allDenominationsMap);
					}
				}
			} catch(Exception ex) {
				scanner.close();
			}
		}
	}
	
	private boolean depositRules(Map<String, String> denominationsMap) {
		boolean rulePassed = true;
		if(denominationsMap.values().stream().filter(n -> Integer.valueOf(n) < 0).count() > 0) {
			System.out.println("Incorrect deposit amount");
			rulePassed = false;
		}
		
		if(!rulePassed && calculateTotalDepositAmount(denominationsMap) == 0f) {
			System.out.println("Deposit amount cannot be zero");
			rulePassed = false;
		}
		return rulePassed;
	}
	
	private void deposit(Map<String, String> denominationsMap, Map<String, String> allDenominationsMap) {
		for(Map.Entry<String, String> entrySet : denominationsMap.entrySet()) {
			allDenominationsMap.put(entrySet.getKey(), String.valueOf(Integer.valueOf(Optional.ofNullable(allDenominationsMap.get(entrySet.getKey())).orElse("0"))+Integer.valueOf(entrySet.getValue())));
		}
	}
	
	private float calculateTotalDepositAmount(Map<String, String> denominationsMap) {
		return denominationsMap.entrySet().stream().map(n -> Float.valueOf(n.getKey())*Integer.valueOf(n.getValue())).reduce(0f, (a,b) -> a+b);
	}
	
	private boolean withdrawalRules(String withdrawalAmount, Map<String, String> allDenominationsMap) {
		boolean rulePassed = true;
		if(Float.valueOf(withdrawalAmount) <= 0f 
			|| Float.valueOf(withdrawalAmount) > calculateTotalDepositAmount(allDenominationsMap)) {
			System.out.println("Incorrect or insufficient funds");
			rulePassed = false;
		}
		return rulePassed;
	}
	
	private Map<String, String> withdrawal(String withdrawalAmount, Map<String, String> allDenominationsMap, List<Integer> availableDenominationsList) {
		Map<String, String> withdrawalMap = new HashMap<>();
		if(RESULT_DISPLAY_AS_INTEGER) {
			int count = 0;
			boolean notDispensable = false;
			int remainingAmount = Integer.valueOf(withdrawalAmount);
			
			while(count < availableDenominationsList.size() && remainingAmount >= availableDenominationsList.get(count)) {
				int denominationValue = availableDenominationsList.get(count++);
				int countOfDenomination = remainingAmount/denominationValue;
				int availableOnGivenDenomination = Integer.valueOf(allDenominationsMap.get(String.valueOf(denominationValue)).toString());
				if(countOfDenomination > availableOnGivenDenomination) {
					notDispensable = true;
				}
				//Do not combine this with the above condition - This part has major role to pay in the business logic for withdrawals
				if(notDispensable && count==availableDenominationsList.size()) {
					notDispensable = true;
				} else if(notDispensable && availableOnGivenDenomination != 0) {
					countOfDenomination = availableOnGivenDenomination;
					remainingAmount = remainingAmount % (availableOnGivenDenomination*denominationValue);
					notDispensable = false;
				} else {
					remainingAmount = remainingAmount % denominationValue;
				}
				if(!notDispensable) {
					withdrawalMap.put(String.valueOf(denominationValue), String.valueOf(countOfDenomination));
				}
			}
			if(notDispensable) {
				System.out.println("Requested withdraw amount is not dispensable");
				withdrawalMap = null;
			}
		}
		return withdrawalMap;
	}
	
	private List<Integer> getAvailableDenominations(Map<String, String> allDenominationsMap) {
		List<Integer> availableDenominationsList = new ArrayList<>();
		for(Map.Entry<String, String> entrySet : allDenominationsMap.entrySet()) {
			if(Integer.parseInt((String)entrySet.getValue()) != 0) {
				availableDenominationsList.add(Integer.parseInt((String)entrySet.getKey()));
			}
		}
		Collections.sort(availableDenominationsList, Collections.reverseOrder());
		return availableDenominationsList;
	}
	
	private void updateAllDenominationsMap(Map<String, String> withdrawalMap, Map<String, String> allDenominationsMap) {
		for(Map.Entry<String, String> entrySet : withdrawalMap.entrySet()) {
			allDenominationsMap.put(entrySet.getKey(), String.valueOf(Integer.parseInt(allDenominationsMap.get(entrySet.getKey())) - Integer.parseInt(entrySet.getValue())));
		}
		System.out.print("Dispensed: ");
		entriesSortedByKeys(withdrawalMap).forEach(es -> System.out.print(es.getKey()+ "s="+ es.getValue() + ", "));
		System.out.println();
		displayBalanceWithEntries(allDenominationsMap);
	}
	
	private Map<String, String> initializeAllDenominationsMap(List<String> denominationsUsed) {
		Map<String, String> allDenominationsMap = new HashMap<>();
		allDenominationsMap.put(denominationsUsed.get(0).toString(), "0");
		allDenominationsMap.put(denominationsUsed.get(1).toString(), "0");
		allDenominationsMap.put(denominationsUsed.get(2).toString(), "0");
		allDenominationsMap.put(denominationsUsed.get(3).toString(), "0");
		return allDenominationsMap;
	}
	
	private void displayBalanceWithEntries(Map<String, String> allDenominationsMap) {
		System.out.print("Balance: ");
		entriesSortedByKeys(allDenominationsMap).forEach(es -> System.out.print(es.getKey() + "s=" + es.getValue() + ", "));
		//DENOMINATIONS_USED.forEach(denomination -> System.out.print(denomination+ "s=" + allDenominationsMap.get(denomination) + ", "));
		System.out.println("Total="+displayAsInteger(calculateTotalDepositAmount(allDenominationsMap), RESULT_DISPLAY_AS_INTEGER));
	}
	
	private <K, V extends Comparable<? super V>> List<Entry<String, String>> entriesSortedByKeys(Map<String, String> map) {
	    List<Entry<String, String>> sortedEntries = new ArrayList<Entry<String, String>>(map.entrySet());
	    Collections.sort(sortedEntries, new Comparator<Entry<String, String>>() {
	        @Override
	        public int compare(Entry<String, String> e1, Entry<String, String> e2) {
	            return Float.valueOf(e2.getKey()).compareTo(Float.valueOf(e1.getKey()));
	        }
	    });
	    return sortedEntries;
	}
	
	private String displayAsInteger(float totalBalance, boolean displayInteger) {
		String total = String.valueOf(totalBalance);
		if(displayInteger) {
			total = String.valueOf((int)totalBalance);
		}
		return total;
	}
}
