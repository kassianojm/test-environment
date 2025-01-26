#!/bin/bash

baseip="172.18.20."




nadj()
{

	op=$1
	card=$2
	echo $op - "op"
	echo $card - "card"

	if [ "$op" == "add" ];then
		op1="up"
	else
		op1="down"
		op="del"
	fi


	for i in $(cat ../NODEFILES/nodefile);
	do
		ip=$( echo $i |cut -d "-" -f2)
		echo "reaching... $baseip"$ip""
		echo $i - "host"
		ssh root@$i "
			echo 1 > /proc/sys/net/ipv4/ip_forward; 
			ifconfig $card $op1; 
			ip addr $op dev $card "$baseip""$ip"/20 broadcast 0.0.0.0
		       "	
	done
}


[ -z $1 ] && echo "Do you want to "add" or "del" ib0 interface?" && exit 0 || echo "Applying!"

[ -z $2 ] && echo "What is the interface?" && exit 0 || echo "Applying interface!"


nadj $1 $2

echo "net adjustament ends"
