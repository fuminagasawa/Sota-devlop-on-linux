import jp.co.nttit.speechrec.Nbest;
import jp.vstone.RobotLib.CPlayWave;
import jp.vstone.RobotLib.CRobotUtil;
import jp.vstone.sotatalk.SpeechRecog;
import jp.vstone.sotatalk.TextToSpeechSota;
import jp.vstone.sotatalk.MotionAsSotaWish;


import java.awt.Color;
import jp.vstone.RobotLib.*;
import java.util.ArrayDeque;


import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class SotaRadicon {

	static final String TAG = "SotaRadicon";

	private static MotionAsSotaWish sotawish;
	private static CSotaMotion motion;


	private static ArrayDeque<sayEntry> sayQueue = new ArrayDeque<>();


	public static CRobotPose GetNeutralPose(){

		CRobotPose pose = new CRobotPose();
		pose.SetPose(new Byte[] {1   ,2   ,3   ,4   ,5   ,6   ,7   ,8},  new Short[]{0   ,-900   ,0   ,900   ,0   ,0   ,0   ,0});
                                           return pose;
	}

	public static void Talk(String message, boolean waitflag){
		CPlayWave.PlayWave(TextToSpeechSota.getTTS(message), waitflag);
	}


	public static void TalkWithSimpleMotion(String message, CRobotPose pose, int playtime, boolean back_neutral){

		motion.play(pose, playtime);
		Talk( message, true);
		motion.waitEndinterpAll();

		if(back_neutral){
			motion.play(GetNeutralPose(),1000);
			motion.waitEndinterpAll();
		}

	}
	public static void TalkWithSimpleMotion(String message, CRobotPose pose, int playtime){
		TalkWithSimpleMotion( message, pose, playtime);
	}

	public static void TalkWithBasicMotion(String message, String scene, int playtime){

		//sotawish.StopIdling();
		//MotionAsSotaWish(CRobotMotion _motion) 

		sayEntry se = new sayEntry();
		se.message = message;
		se.scene = scene;

		sayQueue.add(se);
		//sotawish.SayFile( TextToSpeechSota.getTTSFile(message), scene);
		//sotawish.play( MotionAsSotaWish.MOTION_TYPE_HELLO, 5000);

		//sotawish.StartIdling();


	}



	public static void main(String[] args) {


		HttpServer server;
		CRobotPose pose = new CRobotPose();


		CRobotMem mem = new CRobotMem();
		motion = new CSotaMotion(mem);
		sotawish = new MotionAsSotaWish(motion);


		if (motion == null){
			CPlayWave.PlayWave(TextToSpeechSota.getTTS("モーションコントローラの初期化に失敗しました"),true);
			return;
		}


		//Sota仕様にVSMDを初期化
		motion.InitRobot_Sota();

		PostHandler talkHandler = new PostHandler();
		//talkHandler.sotaRadicon_ = this;

		// ポート 8000 でサーバ起動
		try{
			server = HttpServer.create(new InetSocketAddress(8082), 0);
			// ルートパスにハンドラを登録
			server.createContext("/talk", talkHandler);
			server.createContext("/say", talkHandler);
			server.createContext("/status", talkHandler);
			server.setExecutor(null); // デフォルト Executor
			server.start();
			
		}catch(IOException e){
			return;
		}


        System.out.println("Server started on port 8082");


		//pose = new CRobotPose();
			
		//サーボモータを現在位置でトルクOnにする
		CRobotUtil.Log(TAG, "Servo On");
		motion.ServoOn();
		
		// いったん初期ポーズにする
		pose.setLED_Sota(Color.BLUE, Color.BLUE, 0, Color.BLUE);
		motion.play(GetNeutralPose(),1000);
		motion.waitEndinterpAll();
		
		// お試しポーズ1
		Byte[]  axis_ids       = new Byte[] {1   ,2   ,3   ,4   ,5   ,6   ,7   ,8};	//id
		Short[] axis_values =  new Short[]{-100   , 600,0   ,0,600   ,0   ,0   ,0};	//target pos
		pose.SetPose( axis_ids, axis_values);

		//LEDを点灯（左目：赤、右目：赤、口：Max、電源ボタン：みどり）
		pose.setLED_Sota(Color.WHITE, Color.WHITE, 255, Color.GREEN);
							
		CRobotUtil.Log(TAG, "play:" + motion.play(pose,1000));
		motion.play(pose,1000); //遷移時間1000msecで動作開始。

		Talk("サーバを起動しました", true);
		motion.waitEndinterpAll();  //補間完了まで待つ


		pose.setLED_Sota(Color.BLUE, Color.BLUE, 0, Color.BLUE);
		motion.play(GetNeutralPose(),1000);
		motion.waitEndinterpAll();
			
		// 待機モーション開始
		sotawish.StartIdling();

		while(true){

			try{

				if(0<sayQueue.size()){
					sayEntry se = sayQueue.poll();
					sotawish.Say( se.message, se.scene);
				} 


				CRobotUtil.wait(100);
				Thread.sleep(100);
			}catch(InterruptedException e){
				//isRunning = false;
				break;
			}
		}
		sotawish.StopIdling();



		//サーボモータのトルクオフ
		CRobotUtil.Log(TAG, "Servo Off");
		motion.ServoOff();

	}




	static class sayEntry{

		public String message = "";
		public String scene   = "";

	}

	static class PostHandler implements HttpHandler {

		// "message":"(任意の文字列)" を拾う正規表現
		private static final Pattern pattern_message = Pattern.compile("\"message\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
		private static final Pattern pattern_scene   = Pattern.compile("\"motion\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);

		/**
		 * 文字列中の uXXXX 形式のエスケープを実際の Unicode 文字に置き換える。
		 * u が続かない場合はそのまま出力します。
		 */
		public static String decodeUnicodeEscapes(String input) {
			StringBuilder sb = new StringBuilder(input.length());
			for (int i = 0; i < input.length(); ) {
				char c = input.charAt(i++);
				if (c == '\\' && i < input.length() && input.charAt(i) == 'u') {
					i++;  // 'u' を飛ばす
					// 次の4文字を16進数としてパース
					if (i + 4 <= input.length()) {
						String hex = input.substring(i, i + 4);
						try {
							int codePoint = Integer.parseInt(hex, 16);
							sb.append((char) codePoint);
							i += 4;
						} catch (NumberFormatException e) {
							// 万一不正な16進数なら、そのまま "\\u" + hex として出力
							sb.append("\\u").append(hex);
							i += 4;
						}
					} else {
						// 残り文字数が足りない場合はエスケープ扱いせず出力
						sb.append('\\').append('u');
					}
				} else {
					sb.append(c);
				}
			}
			return sb.toString();
		}



		private String get_value_string(String body, String key){

			Pattern pattern = Pattern.compile(String.format("\"%s\"\\s*:\\s*\"(.*?)\"", key), Pattern.DOTALL);

			// 正規表現で "message" フィールドを抽出
			Matcher m = pattern.matcher(body);
			if (m.find()) {
				String result = decodeUnicodeEscapes(m.group(1));

				System.out.println( String.format("'%s' = %s", key, result));
				return result;
			} else {
				System.out.println( String.format("No '%s' field found in JSON", key));
				return "";
			}

		}



		private void process_talk(String body){


			String message = get_value_string(body, "message");

			System.out.println("Received message: " + message);
			Talk(message, true);

		}

		private void process_say(String body){

			// プリセット文字列
			//System.out.println("MotionAsSotaWish.MOTION_TYPE_BYE      :"+MotionAsSotaWish.MOTION_TYPE_BYE+"/");
			//System.out.println("MotionAsSotaWish.MOTION_TYPE_CALL     :"+MotionAsSotaWish.MOTION_TYPE_CALL+"/");
			//System.out.println("MotionAsSotaWish.MOTION_TYPE_HELLO    :"+MotionAsSotaWish.MOTION_TYPE_HELLO+"/");
			//System.out.println("MotionAsSotaWish.MOTION_TYPE_LOW      :"+MotionAsSotaWish.MOTION_TYPE_LOW+"/");
			//System.out.println("MotionAsSotaWish.MOTION_TYPE_PRESEN_N :"+MotionAsSotaWish.MOTION_TYPE_PRESEN_N+"/");
			//System.out.println("MotionAsSotaWish.MOTION_TYPE_PRESEN_U :"+MotionAsSotaWish.MOTION_TYPE_PRESEN_U+"/");
			//System.out.println("MotionAsSotaWish.MOTION_TYPE_TALK     :"+MotionAsSotaWish.MOTION_TYPE_TALK+"/");




			String message      = get_value_string(body, "message");
			String motion_scene = get_value_string(body, "motion");

			System.out.println("Received message:" + message);
			System.out.println("         motion_scene:" + motion_scene);
			TalkWithBasicMotion( message, motion_scene, 1000);




			/*
			CRobotPose pose = new CRobotPose();
			// お試しポーズ1
			Byte[]  axis_ids       = new Byte[] {1   ,2   ,3   ,4   ,5   ,6   ,7   ,8};	//id
			Short[] axis_values =  new Short[]{-100   , 600,0   ,0,600   ,0   ,0   ,0};	//target pos
			pose.SetPose( axis_ids, axis_values);

			//LEDを点灯（左目：赤、右目：赤、口：Max、電源ボタン：みどり）
			pose.setLED_Sota(Color.WHITE, Color.WHITE, 255, Color.GREEN);
							

			TalkWithSimpleMotion( message, pose, 1000);
			*/
		}


		@Override
		public void handle(HttpExchange exchange) throws IOException {
			// POST 以外は 405
			if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
				exchange.sendResponseHeaders(405, -1);
				return;
			}

			// リクエストボディを文字列として読み取る
			StringBuilder sb = new StringBuilder();
			try (InputStream is = exchange.getRequestBody();
					InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
					BufferedReader reader = new BufferedReader(isr)) {
				String line;
				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}
			}



			// 要求されたURI
			String reqURI = exchange.getRequestURI().toString();
			System.out.println("RequestURI: " + exchange.getRequestURI());

			// リクエストボディ
			String body = decodeUnicodeEscapes(sb.toString());
			System.out.println("LocalAddress: " + exchange.getLocalAddress());
			//System.out.println("body: " + body);


			String responce_str = "OK";

			switch(reqURI){

				case "/talk":
					process_talk( body);
					break;

				case "/say":
					process_say( body);
					break;

				case "/status":

					responce_str = "{\"status\":\"active\"}";

					break;


			}

			// 200 OK 応答
			byte[] response = responce_str.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, response.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(response);
			}
		}
	}	


}