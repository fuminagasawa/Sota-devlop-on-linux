//import jp.co.nttit.speechrec.Nbest;
import jp.vstone.RobotLib.CPlayWave;
import jp.vstone.RobotLib.CRobotUtil;
import jp.vstone.sotatalk.SpeechRecog;
import jp.vstone.sotatalk.TextToSpeechSota;
import jp.vstone.camera.CRoboCamera;
import jp.vstone.camera.CameraCapture;
import java.io.ObjectInputStream.GetField;


import java.awt.Color;
import jp.vstone.RobotLib.*;

import com.sun.net.httpserver.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;

//import System;

public class CameraStreamer {
	static final String TAG = "CameraStreamer";

	private static BufferedImage currentImage;


	public static CRobotPose GetNeutralPose(){

		CRobotPose pose = new CRobotPose();
		pose.SetPose(new Byte[] {1   ,2   ,3   ,4   ,5   ,6   ,7   ,8},  new Short[]{0   ,-900   ,0   ,900   ,0   ,0   ,0   ,0});
		return pose;
	}

	public static void main(String[] args) {


		try{
			// MJPEGストリーミング用サーバ
			HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
			server.createContext("/mjpeg", new MJPEGHandler());
			server.setExecutor(null);
			server.start();
			System.out.println("MJPEG server running on http://localhost:8081/mjpeg");

		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		boolean isRunning = true;

		// 見回しフラグ
		boolean isLookAround = true;


		//VSMDと通信ソケット・メモリアクセス用クラス
		CRobotMem mem = new CRobotMem();
		//Sota用モーション制御クラス
		CSotaMotion motion = new CSotaMotion(mem);

		// Sota仕様にVSMDを初期化
		motion.InitRobot_Sota();

		CRobotPose pose = new CRobotPose();

		
		//サーボモータを現在位置でトルクOnにする
		CRobotUtil.Log(TAG, "Servo On");
		motion.ServoOn();


		try{

			// カメラで撮影
			CRobotUtil.Log(TAG, "Camera Start");
			//撮影用に初期化
			CameraCapture cap = new CameraCapture(CameraCapture.CAP_IMAGE_SIZE_VGA, CameraCapture.CAP_FORMAT_3BYTE_BGR);
			int hCapDev = cap.openDevice("/dev/video0");


			// 撮影＆画像更新スレッド
			new Thread(() -> {
				while (true) {
					
					long hogeStart = System.currentTimeMillis();
					// カメラから画像を取得
					
					try{
						cap.snap();
						currentImage = cap.RawtoBufferedImage();					
					}catch(Exception e){
						CRobotUtil.Log(TAG, "Camera Error:" + e.getMessage());
						break;
					}
					
					
					long hogeEnd = System.currentTimeMillis();

					long hogeTime = hogeEnd - hogeStart;
					//CRobotUtil.Log(TAG, "Capture time:" + Long.toString(hogeTime) + " ms");


					try{
						// 10ms待機
						Thread.sleep(100);
					} catch (InterruptedException e) {
						//isRunning = false;
						break;
					}
				}
			}).start();


		} catch (IOException e) {
			e.printStackTrace();
		}





		// 首振り。メインスレッドでないとサーボモータが動かない
		motion.play(GetNeutralPose(), 1000);
		while (isLookAround) {
					
			// 左を向く
			CRobotUtil.Log(TAG, "look left");
			pose.SetPose(new Byte[] {1, 6},  new Short[]{5, 200});
			//pose.SetPose(new Byte[] {     1,   2,   3,   4,   5,   6,   7,   8},  
			//			  new Short[]{     0, 600,   0,   0, 600, 500,   0,   0});
			motion.play(pose, 3000);
			motion.waitEndinterpAll();

			CRobotUtil.wait(100);

			// 右を向く
			CRobotUtil.Log(TAG, "look right");
			pose.SetPose(new Byte[] {1, 6},  new Short[]{-5, -200});
			//pose.SetPose(new Byte[] {     1,   2,   3,   4,   5,    6,   7,   8},  
			//			  new Short[]{     0, 600,   0,   0, 600, -500,   0,   0});
			motion.play(pose, 3000);
			motion.waitEndinterpAll();


			CRobotUtil.wait(100);
			try{
				Thread.sleep(100);
			}catch(InterruptedException e){
				//isRunning = false;
				break;
			}
		}



		while (isRunning) {
			CRobotUtil.wait(100);
		}
		
		
		CRobotUtil.Log(TAG, "Shutting down ...");
			
		pose.setLED_Sota(Color.WHITE, Color.WHITE, 0, Color.GREEN);
		motion.play(pose,10);			
		//サーボモータのトルクオフ
		CRobotUtil.Log(TAG, "Servo Off");
		motion.ServoOff();
		
		
	}


	static class MJPEGHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", "multipart/x-mixed-replace; boundary=--boundary");
            exchange.sendResponseHeaders(200, 0);
            OutputStream os = exchange.getResponseBody();

            while (true) {
                if (currentImage == null) continue;

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(currentImage, "jpg", baos);
                byte[] jpegData = baos.toByteArray();

                os.write(("--boundary\r\n" +
                         "Content-Type: image/jpeg\r\n" +
                         "Content-Length: " + jpegData.length + "\r\n\r\n").getBytes());
                os.write(jpegData);
                os.write("\r\n".getBytes());
                os.flush();

                try { Thread.sleep(10); } catch (InterruptedException e) {}
            }
        }
    }

}
