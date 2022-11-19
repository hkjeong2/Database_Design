import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Main {
	public static void main(String[] args) throws Exception, ClassNotFoundException, SQLException{
		
		InputStream in = System.in;
		InputStreamReader reader = new InputStreamReader(in);
		BufferedReader br = new BufferedReader(reader);
		Menu menu = new Menu();
		String sInput;
		String subInput;
		
		try {
			Connection conn = 
					DriverManager.getConnection("			" , "			" , "			");
			Statement stmt = conn.createStatement();
			
			while(true) {
				menu.printMainMenu();
				
				sInput = br.readLine();
				
				if(sInput.equals("1")) {		//종목 추천 기능
					menu.printFirstOption();
					subInput = br.readLine();
					
					if(subInput.equals("1")) {			//재무 상황 기반 종목 추천
						ResultSet rset = stmt.executeQuery("select 이름, 매출액, 영업이익, 부채\r\n"
								+ "from 재무 natural join 종목 \r\n"
								+ "where 연도 = 2021 and 영업이익 >\r\n"
								+ "(select avg(영업이익)\r\n"
								+ "from 재무\r\n"
								+ "where 연도 = 2021) \r\n"
								+ "and 부채 < (select avg(부채)\r\n"
								+ "from 재무\r\n"
								+ "where 연도 = 2021)");
						while(rset.next()) {
							System.out.println("이름: " + rset.getString("이름") + "\t" + "매출액: " + rset.getInt("매출액") 
														+ " \t영업이익: " + rset.getInt("영업이익") 
														+ " \t부채: " + rset.getInt("부채"));
						}
						System.out.println("");
					}
					else if(subInput.equals("2")) {			//매수 주문량 기반 종목 추천 
						ResultSet rset = stmt.executeQuery("select 이름, count(이름) as 총매수주문횟수, sum(주문수량) as 총주문수량\r\n"
								+ "from (주문 join 종목 using (종목ID)) join 주문정보 using (주문ID)\r\n"
								+ "where 주문종류 = \"매수\"\r\n"
								+ "group by 이름\r\n"
								+ "order by 총주문수량 DESC\r\n"
								+ "");
						while(rset.next()) {
							System.out.println("이름: " + rset.getString("이름") + " \t총매수주문횟수: " + rset.getString("총매수주문횟수") 
							+ " \t총주문수량: " + rset.getString("총주문수량"));
						}
						System.out.println("");
					}
					else if(subInput.equals("3")) {			//수익률 가장 좋은 고수의 매매 포지션 기반 종목 추천
						ResultSet rset = stmt.executeQuery("select 이름, 주문종류\r\n"
								+ "from (주문 join 종목 using (종목ID)) join 주문정보 using (주문ID)\r\n"
								+ "where 계좌ID = \r\n"
								+ "(select 계좌ID\r\n"
								+ "from 계좌\r\n"
								+ "where 수익률 =\r\n"
								+ "(select max(수익률)\r\n"
								+ "from 계좌))\r\n"
								+ "");
						while(rset.next()) {
							System.out.println("이름: " + rset.getString("이름") + " \t주문종류: " + rset.getString("주문종류"));
						}
						System.out.println("");
					}
				}
				else if(sInput.equals("2")) {			//주식 매매
					menu.printSecondOption();
					subInput = br.readLine();
					
					if(subInput.equals("1")) {		//매수
						int money;
						
						System.out.println("계좌ID: ");
						String accountID = br.readLine();
						System.out.println("종목 이름: ");
						String name = br.readLine();
						System.out.println("주문가격: ");
						int orderPrice = Integer.parseInt(br.readLine());
						System.out.println("주문수량: ");
						int orderQuantity = Integer.parseInt(br.readLine());
						
						ResultSet rset = stmt.executeQuery("select *\r\n"
								+ "from 계좌\r\n"
								+ "where 계좌ID = " + accountID);
						rset.next();
						money = rset.getInt("예수금");
						if (money < orderPrice * orderQuantity)	//주문 총금액과 계좌 예수금 비교 ,
							System.out.println("***예수금 부족***");				//예수금 보다 클 시 매수 주문 불가
						else {					//예수금 보다 적을 시 매수 주문 가능
							ResultSet rsubset = stmt.executeQuery("select * from 가격 natural join 종목"
									+ " where 이름 = \"" + name + "\"");
							
							rsubset.next();
							
							int volume = rsubset.getInt("거래량");
							int volumeUpdated = volume;
							String itemID = rsubset.getString("종목ID");

							int price;
							int quantitySelling;
							int currentPrice = rsubset.getInt("현재가");
							ResultSet rBuyingset = stmt.executeQuery("select * from (가격 natural join 종목) natural join 호가 "
									+ " where 이름 = \"" + name + "\" and 가격 >= " + rsubset.getInt("현재가"));
							
							if (currentPrice <= orderPrice) {		//현재 호가의 가격 보다 주문 가격이 높을 시 ,  
								int tempQuantity = orderQuantity;			//두 가격 사이 호가의 매도 잔량을 주문 수량과 비교 및 차감하며 매수
								
								PreparedStatement pstmtRemoveBid = conn.prepareStatement("delete from 호가 "
										+ "where 가격 = ? and 종목ID = \"" + itemID + "\"");
								PreparedStatement pstmtUpdateBid = conn.prepareStatement("update 호가 set 매도잔량 = ? "
										+ "where 종목ID = \"" + itemID + "\" and 가격 = ?");
								PreparedStatement pstmtUpdateVolume = conn.prepareStatement("update 종목 set 거래량 = ? "
										+ "where 종목ID = \"" + itemID + "\"");
								PreparedStatement pstmtUpdatePrice = conn.prepareStatement("update 가격 set 현재가 = ? "
										+ "where 종목ID = \"" + itemID + "\"");
								PreparedStatement pstmtUpdateMoney = conn.prepareStatement("update 계좌 set 예수금 = ? "
										+ "where 계좌ID = \"" + accountID + "\"");
								
								while(rBuyingset.next()) {	//잔여 호가의 매도잔량을 모두 다 매수하거나 주문 수량 모두가 매수될 때까지
									price = rBuyingset.getInt("가격");
									quantitySelling = rBuyingset.getInt("매도잔량");
											
									if (tempQuantity < quantitySelling) {		//주문 수량이 해당 호가의 매도잔량 보다 적을 시 
										 										//예수금, 호가 상태, 현재가, 상승률, 거래량 update 
											quantitySelling -= tempQuantity;
											volumeUpdated += tempQuantity;
											money -= price * tempQuantity;
											
											pstmtUpdateBid.setInt(1, quantitySelling);
											pstmtUpdateBid.setInt(2, price);
											pstmtUpdateVolume.setInt(1, volumeUpdated);
											pstmtUpdatePrice.setInt(1, price);
											pstmtUpdateMoney.setInt(1, money);
											
											pstmtUpdateBid.executeUpdate();
											pstmtUpdateVolume.executeUpdate();
											pstmtUpdatePrice.executeUpdate();
											pstmtUpdateMoney.executeUpdate();
											
											tempQuantity = -1;
											
											break;
									}
									else if (tempQuantity >= quantitySelling){//주문 수량이 더 많을 시 윗 호가의 매도잔량을 계속 매수
										tempQuantity -= quantitySelling;
										volumeUpdated += quantitySelling;
										money -= price * quantitySelling;
																			//예수금, 호가 상태, 현재가, 상승률, 거래량 update 
										pstmtRemoveBid.setInt(1, price);
										pstmtUpdateVolume.setInt(1, volumeUpdated);
										pstmtUpdatePrice.setInt(1, price);
										pstmtUpdateMoney.setInt(1, money);
										
										pstmtRemoveBid.executeUpdate();
										pstmtUpdateVolume.executeUpdate();
										pstmtUpdatePrice.executeUpdate();
										pstmtUpdateMoney.executeUpdate();
									}
									else if(tempQuantity == 0) {	//주문수량이 매도잔량 수와 딱 맞아 떨어져 모두 매수 체결될 시 그 다음 호가를 현재가로 설정
											ResultSet tempset = stmt.executeQuery("select min(가격) from 호가 natural join 가격 "
													+ "where 가격 >= \"" + price + "\" + 종목ID = \"" + itemID + "\"");
											tempset.next();
											
											pstmtUpdatePrice.setInt(1, tempset.getInt("min(가격)"));
											
											pstmtUpdatePrice.executeUpdate();
											
											break;
										}
								}
								if(tempQuantity > 0) {		//모든 매도잔량을 다 매수하고도 주문수량이 남아 있을 시 새로운 매수대기 호가로 전환
									PreparedStatement pstmtInsertBid = conn.prepareStatement("insert into 호가 values(?,?,?,?)");
									pstmtInsertBid.setString(1, itemID);
									pstmtInsertBid.setInt(2, orderPrice);
									pstmtInsertBid.setInt(3, tempQuantity);
									pstmtInsertBid.setInt(4, 0);
									pstmtUpdatePrice.setInt(1, orderPrice);

									pstmtInsertBid.executeUpdate();
									pstmtUpdatePrice.executeUpdate();
								}
							}	
							else {
								
								boolean bidNone = true;
								ResultSet sSet = stmt.executeQuery("select * from 호가 "
										+ "where 종목ID = \"" + itemID + "\"");
								while(sSet.next()) {
									if(orderPrice == sSet.getInt("가격")) {		//주문가격에 기존 매수잔량 존재 시 기존 값과 주문 수량 더해주기
										PreparedStatement pstmtUpdateBid = conn.prepareStatement("update 호가 set 매수잔량 = ? "
												+ "where 종목ID = \"" + itemID + "\" and 가격 = \"" + orderPrice + "\"");
										pstmtUpdateBid.setInt(1, sSet.getInt("매수잔량") + orderQuantity);
										
										pstmtUpdateBid.executeUpdate();
										bidNone = false;
									}
								}
								if (bidNone == true) {		//주문가격에 호가가 없을 시 새로운 호가 추가
									PreparedStatement pstmtInsertBid = conn.prepareStatement("insert into 호가 values (?,?,?,?)");
									pstmtInsertBid.setString(1, itemID);
									pstmtInsertBid.setInt(2, orderPrice);
									pstmtInsertBid.setInt(3, orderQuantity);
									pstmtInsertBid.setInt(4, 0);
									
									pstmtInsertBid.executeUpdate();
								}
							}
						}
					}
					else if(subInput.equals("2")) {		//검색받은 종목의 호가 조회
						System.out.println("종목 이름: ");
						String name = br.readLine();
						
						ResultSet set = stmt.executeQuery("select * from 호가 natural join 종목"
								+ " where 이름 = \"" + name + "\"");
						while(set.next()) {
							System.out.println("가격: " + set.getInt("가격") + "\t매수잔량: " + set.getInt("매수잔량")
							+ "\t매도잔량: " + set.getInt("매도잔량") + "\t거래량: " + set.getInt("거래량"));
						}
					}
					else if(subInput.equals("3")) {		//검색받은 계좌 조회
						System.out.println("계좌ID: ");
						String accountID = br.readLine();
						
						ResultSet set = stmt.executeQuery("select * from 계좌 "
								+ "where 계좌ID = \"" + accountID + "\"");
						while(set.next()) {
							System.out.println("예수금: " + set.getInt("예수금") + "\t총자산: " + set.getInt("총자산"));
						}
					}
				}
				else if(sInput.equals("3")) break;
			}
			System.out.println("종료합니다");
			
			stmt.close();
			conn.close();
			}
			catch(SQLException sqle) {
			System.out.println("SQLException : "+sqle);
			}
	}
}