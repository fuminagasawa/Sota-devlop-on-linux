import jp.co.nttit.speechrec.Nbest;
import jp.vstone.RobotLib.CPlayWave;
import jp.vstone.RobotLib.CRobotUtil;
import jp.vstone.sotatalk.SpeechRecog;
import jp.vstone.sotatalk.TextToSpeechSota;
import jp.vstone.camera.CRoboCamera;
import jp.vstone.camera.CameraCapture;
import java.io.ObjectInputStream.GetField;


import java.awt.Color;
import jp.vstone.RobotLib.*;


public class CameraSample {
	static final String TAG = "CameraSample";

	public static CSotaMotion initialize_motion(){
		
		CRobotUtil.Log(TAG, "Start " + TAG);

		//VSMDと通信ソケット・メモリアクセス用クラス
		CRobotMem mem = new CRobotMem();
		//Sota用モーション制御クラス
		CSotaMotion motion = new CSotaMotion(mem);
		CRobotUtil.Log(TAG, "Rev. " + mem.FirmwareRev.get());

		if(!mem.Connect()){
			return null;
		}

		return motion;
	}


	public static CRobotPose GetNeutralPose(){

		CRobotPose pose = new CRobotPose();
		pose.SetPose(new Byte[] {1   ,2   ,3   ,4   ,5   ,6   ,7   ,8},  new Short[]{0   ,-900   ,0   ,900   ,0   ,0   ,0   ,0});
                                           return pose;
	}

	public static void main(String[] args) {



		//VSMDと通信ソケット・メモリアクセス用クラス
		CRobotMem mem = new CRobotMem();
		//Sota用モーション制御クラス
		CSotaMotion motion = new CSotaMotion(mem);

		motion.InitRobot_Sota();

		CRoboCamera cam = new CRoboCamera("/dev/video0", motion);
	
		CRobotPose pose = new CRobotPose();

		
		//サーボモータを現在位置でトルクOnにする
		CRobotUtil.Log(TAG, "Servo On");
		motion.ServoOn();
		
		// いったん初期ポーズにする
		pose.setLED_Sota(Color.BLUE, Color.BLUE, 0, Color.BLUE);
		motion.play(pose,10);
		

		CRobotUtil.wait(500);

		// カメラで撮影
		CRobotUtil.Log(TAG, "Camera Start");


		//撮影用に初期化
		CRobotUtil.Log(TAG, "Camera Init");
		cam.initStill(new CameraCapture(CameraCapture.CAP_IMAGE_SIZE_5Mpixel, CameraCapture.CAP_FORMAT_MJPG));


		pose.setLED_Sota(Color.GREEN, Color.GREEN, 0, Color.GREEN);
		motion.play(pose,10);


		CRobotUtil.wait(500);


		CRobotUtil.Log(TAG, "Camera Cap");
		pose.setLED_Sota(Color.RED, Color.RED, 0, Color.GREEN);
		motion.play(pose,10);

		cam.StillPicture("../CameraSample");
		CRobotUtil.Log(TAG, "Camera End");
		


		pose.setLED_Sota(Color.WHITE, Color.WHITE, 0, Color.GREEN);
		motion.play(pose,10);
			
		//サーボモータのトルクオフ
		CRobotUtil.Log(TAG, "Servo Off");
		motion.ServoOff();

	}
}
