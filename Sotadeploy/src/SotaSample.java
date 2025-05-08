import jp.co.nttit.speechrec.Nbest;
import jp.vstone.RobotLib.CPlayWave;
import jp.vstone.RobotLib.CRobotUtil;
import jp.vstone.sotatalk.SpeechRecog;
import jp.vstone.sotatalk.TextToSpeechSota;

import java.awt.Color;
import jp.vstone.RobotLib.*;


public class SotaSample {
	static final String TAG = "SpeechToText";

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



		CSotaMotion motion = initialize_motion();

		if (motion == null){
			CPlayWave.PlayWave(TextToSpeechSota.getTTS("モーションコントローラの初期化に失敗しました"),true);
			return;
		}

		//Sota仕様にVSMDを初期化
		motion.InitRobot_Sota();

		CRobotPose pose = new CRobotPose();
			
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
							
		//CRobotUtil.Log(TAG, "play:" + motion.play(pose,1000));
		motion.play(pose,1000); //遷移時間1000msecで動作開始。


		CPlayWave.PlayWave(TextToSpeechSota.getTTS("こんにちは"),true);
		CPlayWave.PlayWave(TextToSpeechSota.getTTS("僕の名前はSotaです。"),true);
		byte[] data = TextToSpeechSota.getTTS("Text To Speechのテストです。");
		if(data == null){
			CRobotUtil.Log(TAG,"ERROR");
		}
		CPlayWave.PlayWave(data,true);
		motion.waitEndinterpAll();  //補間完了まで待つ



		pose.setLED_Sota(Color.BLUE, Color.BLUE, 0, Color.BLUE);
		motion.play(GetNeutralPose(),1000);
		motion.waitEndinterpAll();
			
		//サーボモータのトルクオフ
		CRobotUtil.Log(TAG, "Servo Off");
		motion.ServoOff();

	}
}
