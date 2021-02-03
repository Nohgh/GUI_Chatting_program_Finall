package application;
//무조선 서버가 먼저 실행된 다음에 클라이언트가 접속하는 것으로 해야함
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;


import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
//클라이언트의 모듈 개발
//클라이언트 프로그램 같은 경우는 서버프로그램 과 다르게 굳이 여러개의 쓰레드가 동시다발절으로 생기는 경우X-->쓰레드 풀 X
//기본적인 쓰레드 
//서버로 메세지를 전달하기 위한 쓰레드, 전달받는 쓰레드 총 2개

//전반적인 클라이언트 구성
//기본적으로 start함수가 실행이 되서 
//접속하기 버튼을 눌렀을떄 
//start클라이언트를 이용해서 현재 서버로 접속을 실행한 이후에 
//어떠한 메세지를 전송하고자 했을때는 보내기 버튼을 눌러  send함수를 이용해서 서버로 어떠한 메세지를 전송하도록 함 
//그와 동시에 서버로 부터 어떠한 메세지를 계속해서 전달을 받아서 전달받은 메세지를 우리의 화면에 출력하도록 만듬
public class Main extends Application 
{
	
	
	Socket socket;
	TextArea textArea;
	
	//클라이언트 프로그램 동작 메소드 
	public void startClient(String IP, int port) //어떤 IP와 port번호 로 접속을 할지 설정해준다.
	{
		Thread thread = new Thread() //쓰레드 객체 사용 
		{
			public void run() 
			{
				try {
					socket = new Socket(IP,port);//try구문 안에서 소켓 초기화 
					receive();//초기화 이후 서버로 부터 메세지를 전달받기 위해 receive()
					
				}catch(Exception e) {
					
					if( !socket.isClosed() ){//오류가 발생한 경우 
						stopClient();//클라이언트 종료 
						System.out.println("[서버 접속 실패]");
						Platform.exit();//프로그램 자체 종료
					}
				}
			}
		};
		thread.start();  
	}
	
	//클라이언트 프로그랢 종료 메소드
	public void stopClient() {
		try {
			if(socket != null && socket.isClosed()) //만약에 소켓이 열려 있는 상태라면 
			{
				socket.close();//소켓객체 자원 해제             
			}
		}
		catch(Exception e) 
		{
			e.printStackTrace(); 
		}
	}
	
	
	//서버로 부터 메세지를 전달 받는 메소드
	public void receive() {
		while(true) {
			try {
				//소켓에서 InputStream을 열어서 현재 서버로 부터 메세지를 전달 받을 수 있게 만들어줌
				InputStream in = socket.getInputStream();
				
				byte[] buffer = new byte[512];//버퍼를 만들어서 총 512바이트 만큼 계속해서 끊어서 버퍼에 담을 수 있게 만들어 준다.
				int length = in.read(buffer);//read함수를 이용해 실제로 입력을 받도록 해준다
				if(length == -1) throw new IOException();//입력 받는 도중 오류가 발생한다면 IOException
				String message = new String(buffer,0,length,"UTF-8");//실제로 버퍼에 있는 정보를 length만큼 메세지라는 변수에 담아서 출력을 시킨다.
				
				Platform.runLater
				(
					()->{ 
						textArea.appendText(message);
						}
				);
			}catch(Exception e) 
			{
				stopClient();
				break;//오류가 있을 경우 클라이언트 종료 후,무한 루프 빠져나옴
			}
		}
	}
	
	//서버로 메세지를 전송하는 메소드
	public void send(String message) 
	{
		
		Thread thread = new Thread() 
		{
			public void run() 
			{
				try 
				{
					OutputStream out = socket.getOutputStream();//메세지 전송
					byte[] buffer = message.getBytes("UTF-8");//보내고자 하는 메세지를 utf-8로 인코딩 해서 보야함(서버에서 전달 받을 때도 utf-8로 받음)
					out.write(buffer);//메세지 전송
					out.flush();//메세지 전송 끝 알리기 
				}
				catch(Exception e)
				{
					stopClient();
				}
			}
		};
		thread.start();
	}
	
	//실제로 프로그램을 동작 시키는 메소드
	@Override
	public void start(Stage primaryStage) 
	{
		BorderPane root = new BorderPane();//레이아웃
		root.setPadding(new Insets(5));//패딩주기
		
		HBox hbox = new HBox();//BoarderPane위에 하나의 레이아웃을 더 넣어줌
		hbox.setSpacing(5);//여백 줌
		
		TextField userName = new TextField();//텍스트공간 생성
		userName.setPrefWidth(150);//텍스트공간 너비-> 150픽셀
		
		userName.setPromptText("닉네임을 입력하세요.");
		HBox.setHgrow(userName,Priority.ALWAYS);//해당 텍스트필드가 출력이 될 수 있도록 만들어줌
		
		TextField IPText = new TextField("127.0.0.1"); //서버의 IP주소가 들어갈 수 있도록 만듬
		TextField portText = new TextField("9876");	//포트 번호 들어가도록 만들어줌
		portText.setPrefWidth(80); 
		
		hbox.getChildren().addAll(userName,IPText,portText);	//실질 적으로 hbox내부에 3개의 텍스트 필드가 만들어 질 수 있게 함
		root.setTop(hbox);//hbox를 위쪽에 달아주도록 만듬
		
		textArea = new TextArea();	//처음 화면이 구성될때 객체 생성
		textArea.setEditable(false);	//내용 수정불가 하게 만듬
		root.setCenter(textArea);	//레이아웃의 중간에 textArea만듬

		TextField input = new TextField(); //입력창이 보이도록 만듬 
		input.setPrefWidth(Double.MAX_VALUE);	//너비 구성 
		input.setDisable(true);	//기본적으로 접속하기 이전에는 어떠한 메세지를 전송할 수 없도록 만들어줌
		
		input.setOnAction(event -> 
		{//어떠한 이벤트가 만들어 졌을때 
			send(userName.getText()+ ": "+ input.getText()+ "\n");//서버로 메세지를 전달할 수 있도록 만들어줌(사용자의 이름과 함께)
			input.setText("");//메세지 전송 이후에 메세지 전송 칸을 비워줌
			input.requestFocus();//다시 어떠한 메세지를 보낼 수 있도록 focus를 설정
		});
		
		Button sendButton = new Button("보내기");//send버튼을 만들어 보내기 버튼을 만든다
		sendButton.setDisable(true);//sendButton은 접속하기 이전에 이용할 수 없도록 Disable
		
		sendButton.setOnAction( event -> 
		{//sendButton을 누르면 이벤트 처리
			send(userName.getText()+ ": "+input.getText()+"\n");//enter버튼을 눌러서 전송가능,버튼을 눌렀을떄도 메세지가 전송할 수 있도록 만들어줌
			input.setText("");
			input.requestFocus();
		});
		
		Button connectionButton = new Button("접속하기"); //접속하기 인 connection버튼 
		connectionButton.setOnAction(// 맨 처음에 서버와 연결을 해서 접속을 하는것 
			event -> //버튼을 눌렀을떄 이벤트 처리 
			{
				if(connectionButton.getText().equals( "접속하기" )) //만약 connectionButton의 내용이 접속하기 이면 
				{
					int port = 9876;	//포트번호 설정
					try 
					{
						port = Integer.parseInt(portText.getText());
						//실제로 사용자가 입력한 그 포트번호 입력칸 내용에 들어있는 텍스트 내용을 정수 형태로 변환해서 다시 담을 수 있게 해줌
						//기본적으로는 9876으로 설정되어 있고 사용자가 어떤 별도의 포트번호를 입력하게 되면 
						//그 포트로 접속이 이루어 지도록 만들어 준다. 
					}
					catch(Exception e) 
					{
						e.printStackTrace();
					}
					startClient(IPText.getText(),port);//startClient함수를 이용해서 실제로 특정한 IP주소에 어떠한 포트번호로 접속할 수 있도록 만들어 준다.
					
					Platform.runLater( 	()-> //화면에 관련된 내용이 출력 될 수 있도록 만들어 준다.
					{
						textArea.appendText("[채팅방 접속]\n");
					});
						connectionButton.setText("종료하기");//실제로 접속이 이루어졌음으로 connection버튼을 이용해서 내용을 종료하기로 바꿔줌
						input.setDisable(false);//setDisable을 false로 입력해서 사용자가 직접 값을 입력하고 버튼을 눌러서 메세지를 전송할 수 있도록함 
						sendButton.setDisable(false);
						input.requestFocus();//바로 어떠한 메세지를 줄 수 있도록  focusing를 줌 
				}
				
				else //버튼을 눌렀을떄 종료하기 버튼 이였다면 
				{
					stopClient();//클라이언트 기능을 종료 
					
					Platform.runLater(	() -> {//디자인 요소를 출력 
						textArea.appendText("[ 채팅방 퇴장 ]");
					});
					
					connectionButton.setText("접속하기");//다시 버튼의 내용을 접속하기로 바꿈 
					input.setDisable(true); //다시 입력칸 및 버튼을 누를 수 없도록 설정 
					sendButton.setDisable(true);
				}
			});
		BorderPane pane = new BorderPane();
		pane.setLeft(connectionButton);//펜의 왼쪽에 connection버튼이 들어가도록 만듬 (접속하기 버튼)
		pane.setCenter(input);//사용자가 어떠한 값을 입력할 수 있도록 만듬 
		pane.setRight(sendButton);//보내기 버튼 
		
		root.setBottom(pane);//전체 루트 레이아웃에 아래쪽에 방금 만든 BorderPane이 들어가도록 만들어줌 
		
		Scene scene = new Scene(root,400,400);//씬 만들어줌 
		
		primaryStage.setTitle("[채팅 클라이언트 ]");//primaryStage를 만들어서 대략적인 정보를 담을 수 있도록 만들어줌 
		primaryStage.setScene(scene);
		primaryStage.setOnCloseRequest(event -> stopClient());//사용자가 화면의 닫기 버튼을 눌렀다면  stopClient을 실행한 이후에 종료가 이루어 지도록 만든다.
		primaryStage.show();//세팅이 다 끝난 후 놓여지도록 만듬 
		
		connectionButton.requestFocus();//requestFocus에서 기본적으로 프로그램이 실행이 되면 cnnection버튼인 접속하기버튼이 포커싱이 되도록 만들어줌
	}//start()
	
	//프로그램 진입점
	public static void main(String[] args) {
		launch(args);
	}
}
