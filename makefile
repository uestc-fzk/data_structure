run:
	java -jar target/data_structure-1.0-SNAPSHOT.jar
run-ea: # 开断言
	java -ea -jar target/data_structure-1.0-SNAPSHOT.jar
git-proxy:
	git config --global http.proxy http://127.0.0.1:7890
	git config --global https.proxy https://127.0.0.1:7890
	git config --global -l
git-unproxy:
	git config --global --unset http.proxy
	git config --global --unset https.proxy
	git config --global -l
msg="auto"
git-push:
	git add .
	git commit -m "${msg}"
	git push origin main