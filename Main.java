package application;
//모든 라이브러리는 설치할떄 java fx로 import하기 (awt 아님)
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;

//하나의 서버프로그램은 오직 하나의 서버모듈을 구동시키도록 개발을 해볼것이다.
public class Main extends Application 
{
	public static ExecutorService threadPool;
	//ExecutorService 라이브러리 :여러개의 쓰레드를 효율적으로 관리하기 위해 사용하는 대표적인 라이브러리 
	
	//쓰레드풀로 쓰레드들을 관리하게 된다면 기본적인 쓰레드에 제한을 두기 때문에 갑작스럽게 클라이언트 숫자가 폭증하더라도 쓰레드들의 숫자에 제한이 있기때문에 서버에 성능 저하를 감소할 수 있다.
	
	//즉, 한정된 자원을 이용해서 안정적으로 서버를 운영하기 위해 쓰레드 풀 기법을 이용한다.
	
	//쓰레드 풀 -> 다양한 클라이언트가 접속했을 때 쓰레드 들을 효과적으로 관리할 수 있도록 해준다.
	
	
	//Vector : 일종의 배열, 더 쉽게 사용할 수 있는 배열
	public static Vector<Client> clients = new Vector<Client>();//arraylist와 같은 구조로 이루어져 있다.
	//일단 이건 빈 벡터 즉 빈 배열과 같은 코드
	
	ServerSocket serverSocket;	//서버소켓
	
	//서버를 구동시켜 클라이언트의 연결을 기다리는 메소드
	public void startServer(String IP, int port)//어떠한 IP로,어떠한 port를 열어서 통신을 할건지 적어주는것  
	{
		try 
		{
			serverSocket = new ServerSocket();	//서버가 실행이 되면 서버소켓 부터 만들어 준다
			serverSocket.bind(new InetSocketAddress(IP, port)); 
			//바인딩: 각종 값들이 확정되어 더 이상 변경할 수 없는 구속(bind) 상태가 되는 것.
			//기본적으로 소켓통신은 소켓에 대한 객체를 활성해주고 bind라는 것을 해서 서버컴퓨터역활을 수행하는 그 컴퓨터가 자신의 ip주소,포트번호로 특정한 쿨라이언트의 접속을 기다리도록 할 수 있다.
			//InetSocketAddress(IP, port): IP 주소와 포트 번호로부터 소켓 주소를 작성합니다.

		}
		catch(Exception e) 
		{//오류가 발생하는 경우라면 
			e.printStackTrace();
			if(!serverSocket.isClosed()) //서버소켓이 닫쳐있는 상태가 아니라면 stopServer메소드를 이용해 서버종료해준다.
			{
				stopServer();
			}
			return;
		}
		//오류발생X, 성공적으로 서버가 소켓을 잘열어서 기다릴 수 있는 생태가 되었다면 
		//클라이언트가 접속할 떄 까지 계속 기다리면 된다.
		
		//클라이언트가 접속할 떄 까지 계속 기다리는 쓰레드
		//익명 구현 객체 
		//인터페이스 변수 = new 인터페이스(){ ... };
		Runnable thread = new Runnable() {

			@Override
			public void run() 
			{
				while(true) 
				{//while(true)이용해서 계속해서 새로운 클라이언트가 접속할 수 있도록 만들어준다.
					try {
						Socket socket = serverSocket.accept();
						clients.add(new Client(socket));
						//클라이언트가 접속을 했다면 클라이언트(clients) 배열에 새롭게 접속한 클라이언트(new Client)를 초기화 해준 후 추가 해주는 것이다.
						
						System.out.println("[클라이언트 접속" //일종의 로그를 출력해준다.
						+socket.getRemoteSocketAddress()	//접속한 클라이언트의 주소를 출력
						+": "+ Thread.currentThread().getName());//해당 쓰레드 정보 출력
						
					}catch(Exception e) 
					{	//오류가 발생했다면 
						if(!serverSocket.isClosed()) {//서버소켓에 문제가 발생한 것이므로 
							stopServer(); //서버를 작동중지 시키고
						}
						break; //break로 빠져나온다. //16.36
					}
				}//while
			}//run
			
		};//runnable
	  threadPool = Executors.newCachedThreadPool();	//1.쓰레드 풀 먼저 초기화 
	  threadPool.submit(thread);	//2. 초기화 해준 쓰레드풀에 첫번쨰 클라이언트로써 접속을 기다리는 쓰레드를 넣어준다(위에서 만든 쓰레드).
	}//startServer()
	
	//서버의 작동을 중지시키는 메소드 (서버 작동 종료 이후에 전체 자원을 할당해주는 메소드 )->모든 클라이언트에 대한 정보를 끊어 주면 된다.->모든 소켓 닫기
	public void stopServer() {	//각종 예외를 처리하는 메소드의 작성 여부가 서버프로그램의 품질의 큰영향을 미친다
		try 
		{
			//현재 작동중인 모든 소켓 닫기
			Iterator<Client> iterator = clients.iterator(); //Iterator을 이용해 모든 클라이언트에 개별적으로 접근할 수 있게 해준다.
			//Iterator는 자바의 컬렉션 프레임워크에서 컬렉션[ (set,list) -> (vector: List) ]에 저장되어 있는 요소들을 읽어오는 방법을 표준화한 것이다.

			while(iterator.hasNext()) //하나씩 접근할 수 있도록 해준다
			{ //hasNext() : 읽어올 요소가 남아있는지 확인하는 메소드이다. 요소가 있으면 true, 없으면 false
				Client client = iterator.next();//특정한 클라이언트에 접근해서
				client.socket.close();//그 클라이언트의 소켓을 닫아버린다.
				iterator.remove();//iterator에서도 해당 연결이 끊긴 클라이언트를 제거해준다.
			}
			//모든 클라이언트에 대한 연결이 끊겼음으로 서버소켓 객체도 닫는다.
			if(serverSocket != null && !serverSocket.isClosed()) {
				serverSocket.close();
			}
			//쓰레드 풀 종료 (자원을 할당 해제)
			if(threadPool != null && !threadPool.isShutdown()) {
				threadPool.shutdown();
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}//stopServer()

	//UI를 동작시키고 , 실질적으로 프로그램을 동작시키는  메소드 (내가 디자인하고 띄울것이기 떄문에 기존에 생성되어있던 것들 지워줌)
	@Override
	public void start(Stage primaryStage) //import javafx.stage.Stage; 
	{
		//한개의 전체 디자인 틀을 담을 수 있는 펜을 만든다.
		BorderPane root = new BorderPane();//레이아웃을 만들어서 전체적인 디자인 요소들을 담을 수 있도록 해준다.
		//BorderPane : setCenter,setBottom 이런식으로 레이아웃에서 중간,아래 위치로 다양한 요소를 담아서 처리할 수 있도록 만든다.
		
		root.setPadding(new Insets(5));//내부에 5만큼 패딩을 준다.
		 
		TextArea textArea = new TextArea();	//어떠한 긴 문장의 텍스트가 담길 수 있는 공간 
		textArea.setEditable(false);	//어떠한 문장을 출력만 하고 우리가 그 문장을 채울 수 없도록 만든다.(수정이 불가능 하도록 만든다.)
		textArea.setFont(new Font("새굴림", 20));	//폰트 적용( 컴에 설치 되어있는 글씨체 사용)
		root.setCenter(textArea);	//중간에 textArea담을 수 있게 함
		
		Button toggleButton = new Button("시작하기");		//서버 작동을 시작하도록 만드는 버튼 
		//toggleButton: 일종의 스위치 ,시작을 한다음에는 다시 종료할 수 있도록 그 버튼의 내용이 바뀌는 식
		toggleButton.setMaxWidth(Double.MAX_VALUE);
		BorderPane.setMargin(toggleButton, new Insets(1, 0, 0, 0));//디자인을 예쁘게 해줌
		root.setBottom(toggleButton); //setBottom: 버튼을 담을 수 있게 해준다.
		
		String IP = "127.0.0.1";	//127.0.0.1 : 자기자신의 컴퓨터 주소를 의미(로컬주소,루프백 주소)
								//실제로 서버를 운영하는것은 아니기 떄문에 컴안에서 테스트해보겠다는 목적으로 만든것 
		int port = 9876;
//--------------------------------------------------------------------------------------------------------------------
		
		toggleButton.setOnAction(event -> { //toggleBotton을 눌렀을떄(event)에 대해서 Action처리
			
				if(toggleButton.getText().equals("시작하기")) {//만약 토글버튼이 "시작하기"를 포함하고 있는 문자라면 
					startServer(IP,port); //서버를 시작 시켜줌
					Platform.runLater(
						( )-> { 
						//java fx와 같은 경우는 버튼을 눌렀을떄 바로 textArea에 어떠한 정보를 쓰도록 하면 안되고
						// 항상 runLator과 같은 함수를 이용해서 어떠한 GUI요소를 출력할 수 있도록 만들어 준다.
						String message = String.format("[서버 시작]\n ", IP,port); //서버시작 이라고 메세지를 출력하도록 만들어 줌 
						textArea.appendText(message); //이후에 textArea에 비로소 message를 출력하도록 만들어 준다
						toggleButton.setText("종료하기");//이후에 토글버튼의 내용을 종료하기로 바꿔준다.
						//( 버튼: 시작하기--> 종료하기 )
						}
					);//runLater()
				}//if
				else {
					//버튼을 눌렀을떄 시작하기가 아니라면 종료하기 버튼의 상태였던것 이므로 종료버튼을 눌렀을떄는 서버를 종료시킴
					stopServer();
					Platform.runLater(
							( )-> { 
							String message = String.format("[서버 종료]\n ", IP,port); //서버종료 이라고 메세지를 출력하도록 만들어 줌 
							textArea.appendText(message); //이후에 textArea에 비로소 message를 출력하도록 만들어 준다
							toggleButton.setText("시작하기");//이후에 토글버튼의 내용을 시작하기로 바꿔준다.
							}
						);//runLater()
					}
			}//event
		);//setOnAction
	Scene scene = new Scene(root, 400,400);	//화면 구성, 400x400해상도
	
	primaryStage.setTitle("[채팅 서버]");	//primaryStage-->기본적으로 우리 프로그램의 정보를 출력하도록 만듬 
	primaryStage.setOnCloseRequest( event -> stopServer() ); //종료버튼을 눌렀다면 stopServer함수를 실행후에 종료할 수 있도록 함
	primaryStage.setScene(scene);//위에서 만든 scene정보를 화면에 정상적으로 출력할 수 있도록 primaryStage에 scene정보를 설정해줌
	primaryStage.show();//우리 화면에 출력
	
	}//start()
	
	//프로그램의 진입점입니다.
	public static void main(String[] args) 
	{
		launch(args);
	}
}


   



















