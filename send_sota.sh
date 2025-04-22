
# Sotaにsrc以下のファイルを送り込む
sota_ip="192.168.0.1"

# SotaのIPアドレスをコンソールで入力してsota_ipに格納
read -p "Enter Sota's IP address: " sota_ip

# 入力されたIPアドレスで接続できるか確認
ping -c 1 $sota_ip
if [ $? -ne 0 ]; then
    echo "Sota is not reachable. Please check the IP address."
    exit 1
fi


# SCPを使ってSotaにファイルを送信(passwordは"edison00")
# 送信先のディレクトリは"/home/root/SotaApps/src/"に指定
# 送信元のディレクトリは"./src/"に指定
# sshpassを使ってパスワードを指定
sshpass -p edison00 scp -r ./deploy/* root@$sota_ip:/home/root/SotaDeploy/
echo "Files have been sent to SotaDeploy directory."

# 送信した先で、*.shファイルのパーミッションに実行権限を付与
sshpass -p edison00 ssh root@$sota_ip "chmod +x /home/root/SotaDeploy/deploy/*.sh"




# 送信終わったよのメッセージをecho
echo "Files have been sent to Sota"
