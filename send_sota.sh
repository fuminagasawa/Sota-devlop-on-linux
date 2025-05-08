
# Sotaにsrc以下のファイルを送り込む

# SotaのIPアドレスをsota_ipファイルから読みだしてsota_ipに格納
# もしsota_ipファイルが存在しない場合は、コンソールからIPアドレスを入力してsota_ipに格納
if [ -f "sota_ip.conf" ]; then
    sota_ip=$(cat sota_ip.conf)
else
    read -p "Enter Sota's IP address: " sota_ip
    echo $sota_ip > sota_ip
fi

# sota_ipを表示
echo "Sota's IP address is: $sota_ip"

# SotaのIPアドレスをコンソールで入力してsota_ipに格納
#read -p "Enter Sota's IP address: " sota_ip

# 入力されたIPアドレスで接続できるか確認
ping -c 1 $sota_ip > /dev/null
if [ $? -ne 0 ]; then
    echo "Sota is not reachable. Please check the IP address."
    exit 1
fi


# SCPを使ってSotaにファイルを送信(passwordは"edison00")
# 送信先のディレクトリは"/home/root/SotaApps/src/"に指定
# 送信元のディレクトリは"./src/"に指定
# sshpassを使ってパスワードを指定する。
# エラーが発生した場合は、エラーの詳細を表示する。
sshpass -p edison00 scp -r ./SotaDeploy root@$sota_ip:/home/root/ 2> error.log
if [ $? -ne 0 ]; then
    echo "Error occurred while sending files. Check error.log for details."
    exit 1
fi

echo "Files have been sent to SotaDeploy directory."

# 送信したファイルの改行コードをLFに統一
# パスワードedison00を使ってユーザーrootでSotaにssh接続
sshpass -p edison00 ssh -tt root@$sota_ip << EOF
cd /home/root/SotaDeploy
find . -type f -exec sed -i 's/\r$//' {} +
exit
EOF



# 送信した先で、*.shファイルのパーミッションに実行権限を付与
sshpass -p edison00 ssh root@$sota_ip "chmod +x /home/root/SotaDeploy/*.sh"

# 送信終わったよのメッセージをecho
echo "Files have been sent to Sota"
