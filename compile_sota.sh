
# Sotaにsrc以下のファイルを送り込む

# SotaのIPアドレスをsota_ipファイルから読みだしてsota_ipに格納
# もしsota_ipファイルが存在しない場合は、コンソールからIPアドレスを入力してsota_ipに格納
if [ -f "sota_ip" ]; then
    sota_ip=$(cat sota_ip)
else
    read -p "Enter Sota's IP address: " sota_ip
    echo $sota_ip > sota_ip
fi

# sota_ipを表示
echo "Sota's IP address is: $sota_ip"

# SotaのIPアドレスをコンソールで入力してsota_ipに格納
#read -p "Enter Sota's IP address: " sota_ip

# 入力されたIPアドレスで接続できるか確認
ping -c 1 $sota_ip > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "Sota is not reachable. Please check the IP address."
    exit 1
fi


# パスワードedison00を使ってユーザーrootでSotaにssh接続
sshpass -p edison00 ssh -tt root@$sota_ip << EOF
cd /home/root/SotaDeploy
./compile_in_sota.sh $1
exit
EOF

# コンパイル終わったよのメッセージをecho
echo "Compilation has been completed on Sota."