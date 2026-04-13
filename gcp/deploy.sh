#!/bin/bash
# ============================================================
# 部署腳本（在本機執行）
# 用法：GCP_VM_IP=1.2.3.4 GCP_USER=yourname ./gcp/deploy.sh
# ============================================================
set -e

GCP_VM_IP="${GCP_VM_IP:?請設定 GCP_VM_IP 環境變數，例如：export GCP_VM_IP=1.2.3.4}"
GCP_USER="${GCP_USER:-$USER}"

echo "==> 建置 JAR..."
cd "$(dirname "$0")/../backend"
mvn clean package -DskipTests -q
JAR=$(ls target/*.jar | head -1)

echo "==> 上傳 JAR 到 VM (${GCP_USER}@${GCP_VM_IP})..."
scp "$JAR" "${GCP_USER}@${GCP_VM_IP}:/opt/mahjong/app.jar"

echo "==> 重啟服務..."
ssh "${GCP_USER}@${GCP_VM_IP}" "sudo systemctl restart mahjong-backend"

echo "==> 等待服務啟動（15秒）..."
sleep 15

echo "==> 健康檢查..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://${GCP_VM_IP}:8080/health" || echo "000")
if [ "$HTTP_CODE" = "200" ]; then
  echo "✅ 部署成功！服務運行中。"
else
  echo "⚠️  健康檢查回傳 $HTTP_CODE，請查看 logs："
  echo "   ssh ${GCP_USER}@${GCP_VM_IP} 'sudo journalctl -u mahjong-backend -n 50'"
fi
