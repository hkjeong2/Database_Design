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
				
				if(sInput.equals("1")) {		//���� ��õ ���
					menu.printFirstOption();
					subInput = br.readLine();
					
					if(subInput.equals("1")) {			//�繫 ��Ȳ ��� ���� ��õ
						ResultSet rset = stmt.executeQuery("select �̸�, �����, ��������, ��ä\r\n"
								+ "from �繫 natural join ���� \r\n"
								+ "where ���� = 2021 and �������� >\r\n"
								+ "(select avg(��������)\r\n"
								+ "from �繫\r\n"
								+ "where ���� = 2021) \r\n"
								+ "and ��ä < (select avg(��ä)\r\n"
								+ "from �繫\r\n"
								+ "where ���� = 2021)");
						while(rset.next()) {
							System.out.println("�̸�: " + rset.getString("�̸�") + "\t" + "�����: " + rset.getInt("�����") 
														+ " \t��������: " + rset.getInt("��������") 
														+ " \t��ä: " + rset.getInt("��ä"));
						}
						System.out.println("");
					}
					else if(subInput.equals("2")) {			//�ż� �ֹ��� ��� ���� ��õ 
						ResultSet rset = stmt.executeQuery("select �̸�, count(�̸�) as �Ѹż��ֹ�Ƚ��, sum(�ֹ�����) as ���ֹ�����\r\n"
								+ "from (�ֹ� join ���� using (����ID)) join �ֹ����� using (�ֹ�ID)\r\n"
								+ "where �ֹ����� = \"�ż�\"\r\n"
								+ "group by �̸�\r\n"
								+ "order by ���ֹ����� DESC\r\n"
								+ "");
						while(rset.next()) {
							System.out.println("�̸�: " + rset.getString("�̸�") + " \t�Ѹż��ֹ�Ƚ��: " + rset.getString("�Ѹż��ֹ�Ƚ��") 
							+ " \t���ֹ�����: " + rset.getString("���ֹ�����"));
						}
						System.out.println("");
					}
					else if(subInput.equals("3")) {			//���ͷ� ���� ���� ����� �Ÿ� ������ ��� ���� ��õ
						ResultSet rset = stmt.executeQuery("select �̸�, �ֹ�����\r\n"
								+ "from (�ֹ� join ���� using (����ID)) join �ֹ����� using (�ֹ�ID)\r\n"
								+ "where ����ID = \r\n"
								+ "(select ����ID\r\n"
								+ "from ����\r\n"
								+ "where ���ͷ� =\r\n"
								+ "(select max(���ͷ�)\r\n"
								+ "from ����))\r\n"
								+ "");
						while(rset.next()) {
							System.out.println("�̸�: " + rset.getString("�̸�") + " \t�ֹ�����: " + rset.getString("�ֹ�����"));
						}
						System.out.println("");
					}
				}
				else if(sInput.equals("2")) {			//�ֽ� �Ÿ�
					menu.printSecondOption();
					subInput = br.readLine();
					
					if(subInput.equals("1")) {		//�ż�
						int money;
						
						System.out.println("����ID: ");
						String accountID = br.readLine();
						System.out.println("���� �̸�: ");
						String name = br.readLine();
						System.out.println("�ֹ�����: ");
						int orderPrice = Integer.parseInt(br.readLine());
						System.out.println("�ֹ�����: ");
						int orderQuantity = Integer.parseInt(br.readLine());
						
						ResultSet rset = stmt.executeQuery("select *\r\n"
								+ "from ����\r\n"
								+ "where ����ID = " + accountID);
						rset.next();
						money = rset.getInt("������");
						if (money < orderPrice * orderQuantity)	//�ֹ� �ѱݾװ� ���� ������ �� ,
							System.out.println("***������ ����***");				//������ ���� Ŭ �� �ż� �ֹ� �Ұ�
						else {					//������ ���� ���� �� �ż� �ֹ� ����
							ResultSet rsubset = stmt.executeQuery("select * from ���� natural join ����"
									+ " where �̸� = \"" + name + "\"");
							
							rsubset.next();
							
							int volume = rsubset.getInt("�ŷ���");
							int volumeUpdated = volume;
							String itemID = rsubset.getString("����ID");

							int price;
							int quantitySelling;
							int currentPrice = rsubset.getInt("���簡");
							ResultSet rBuyingset = stmt.executeQuery("select * from (���� natural join ����) natural join ȣ�� "
									+ " where �̸� = \"" + name + "\" and ���� >= " + rsubset.getInt("���簡"));
							
							if (currentPrice <= orderPrice) {		//���� ȣ���� ���� ���� �ֹ� ������ ���� �� ,  
								int tempQuantity = orderQuantity;			//�� ���� ���� ȣ���� �ŵ� �ܷ��� �ֹ� ������ �� �� �����ϸ� �ż�
								
								PreparedStatement pstmtRemoveBid = conn.prepareStatement("delete from ȣ�� "
										+ "where ���� = ? and ����ID = \"" + itemID + "\"");
								PreparedStatement pstmtUpdateBid = conn.prepareStatement("update ȣ�� set �ŵ��ܷ� = ? "
										+ "where ����ID = \"" + itemID + "\" and ���� = ?");
								PreparedStatement pstmtUpdateVolume = conn.prepareStatement("update ���� set �ŷ��� = ? "
										+ "where ����ID = \"" + itemID + "\"");
								PreparedStatement pstmtUpdatePrice = conn.prepareStatement("update ���� set ���簡 = ? "
										+ "where ����ID = \"" + itemID + "\"");
								PreparedStatement pstmtUpdateMoney = conn.prepareStatement("update ���� set ������ = ? "
										+ "where ����ID = \"" + accountID + "\"");
								
								while(rBuyingset.next()) {	//�ܿ� ȣ���� �ŵ��ܷ��� ��� �� �ż��ϰų� �ֹ� ���� ��ΰ� �ż��� ������
									price = rBuyingset.getInt("����");
									quantitySelling = rBuyingset.getInt("�ŵ��ܷ�");
											
									if (tempQuantity < quantitySelling) {		//�ֹ� ������ �ش� ȣ���� �ŵ��ܷ� ���� ���� �� 
										 										//������, ȣ�� ����, ���簡, ��·�, �ŷ��� update 
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
									else if (tempQuantity >= quantitySelling){//�ֹ� ������ �� ���� �� �� ȣ���� �ŵ��ܷ��� ��� �ż�
										tempQuantity -= quantitySelling;
										volumeUpdated += quantitySelling;
										money -= price * quantitySelling;
																			//������, ȣ�� ����, ���簡, ��·�, �ŷ��� update 
										pstmtRemoveBid.setInt(1, price);
										pstmtUpdateVolume.setInt(1, volumeUpdated);
										pstmtUpdatePrice.setInt(1, price);
										pstmtUpdateMoney.setInt(1, money);
										
										pstmtRemoveBid.executeUpdate();
										pstmtUpdateVolume.executeUpdate();
										pstmtUpdatePrice.executeUpdate();
										pstmtUpdateMoney.executeUpdate();
									}
									else if(tempQuantity == 0) {	//�ֹ������� �ŵ��ܷ� ���� �� �¾� ������ ��� �ż� ü��� �� �� ���� ȣ���� ���簡�� ����
											ResultSet tempset = stmt.executeQuery("select min(����) from ȣ�� natural join ���� "
													+ "where ���� >= \"" + price + "\" + ����ID = \"" + itemID + "\"");
											tempset.next();
											
											pstmtUpdatePrice.setInt(1, tempset.getInt("min(����)"));
											
											pstmtUpdatePrice.executeUpdate();
											
											break;
										}
								}
								if(tempQuantity > 0) {		//��� �ŵ��ܷ��� �� �ż��ϰ� �ֹ������� ���� ���� �� ���ο� �ż���� ȣ���� ��ȯ
									PreparedStatement pstmtInsertBid = conn.prepareStatement("insert into ȣ�� values(?,?,?,?)");
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
								ResultSet sSet = stmt.executeQuery("select * from ȣ�� "
										+ "where ����ID = \"" + itemID + "\"");
								while(sSet.next()) {
									if(orderPrice == sSet.getInt("����")) {		//�ֹ����ݿ� ���� �ż��ܷ� ���� �� ���� ���� �ֹ� ���� �����ֱ�
										PreparedStatement pstmtUpdateBid = conn.prepareStatement("update ȣ�� set �ż��ܷ� = ? "
												+ "where ����ID = \"" + itemID + "\" and ���� = \"" + orderPrice + "\"");
										pstmtUpdateBid.setInt(1, sSet.getInt("�ż��ܷ�") + orderQuantity);
										
										pstmtUpdateBid.executeUpdate();
										bidNone = false;
									}
								}
								if (bidNone == true) {		//�ֹ����ݿ� ȣ���� ���� �� ���ο� ȣ�� �߰�
									PreparedStatement pstmtInsertBid = conn.prepareStatement("insert into ȣ�� values (?,?,?,?)");
									pstmtInsertBid.setString(1, itemID);
									pstmtInsertBid.setInt(2, orderPrice);
									pstmtInsertBid.setInt(3, orderQuantity);
									pstmtInsertBid.setInt(4, 0);
									
									pstmtInsertBid.executeUpdate();
								}
							}
						}
					}
					else if(subInput.equals("2")) {		//�˻����� ������ ȣ�� ��ȸ
						System.out.println("���� �̸�: ");
						String name = br.readLine();
						
						ResultSet set = stmt.executeQuery("select * from ȣ�� natural join ����"
								+ " where �̸� = \"" + name + "\"");
						while(set.next()) {
							System.out.println("����: " + set.getInt("����") + "\t�ż��ܷ�: " + set.getInt("�ż��ܷ�")
							+ "\t�ŵ��ܷ�: " + set.getInt("�ŵ��ܷ�") + "\t�ŷ���: " + set.getInt("�ŷ���"));
						}
					}
					else if(subInput.equals("3")) {		//�˻����� ���� ��ȸ
						System.out.println("����ID: ");
						String accountID = br.readLine();
						
						ResultSet set = stmt.executeQuery("select * from ���� "
								+ "where ����ID = \"" + accountID + "\"");
						while(set.next()) {
							System.out.println("������: " + set.getInt("������") + "\t���ڻ�: " + set.getInt("���ڻ�"));
						}
					}
				}
				else if(sInput.equals("3")) break;
			}
			System.out.println("�����մϴ�");
			
			stmt.close();
			conn.close();
			}
			catch(SQLException sqle) {
			System.out.println("SQLException : "+sqle);
			}
	}
}