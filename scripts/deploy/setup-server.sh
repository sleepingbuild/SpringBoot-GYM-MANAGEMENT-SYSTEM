#!/usr/bin/env bash
# setup-server.sh - Chuan bi may chu Ubuntu/Debian MOT LAN de Jenkins deploy qua SSH. Chay tren may chu:
#   bash setup-server.sh
# Can sudo va Internet. Cai Docker, git, curl, SSH server; them tai khoan hien tai vao nhom docker; mo cong.
set -euo pipefail

[ "$(id -u)" -eq 0 ] && { echo "Hay chay bang tai khoan thuong co sudo, khong chay bang root."; exit 1; }

echo "== Cai goi can thiet"
sudo apt-get update -y
sudo apt-get install -y git curl ca-certificates openssh-server

if ! command -v docker >/dev/null 2>&1; then
  echo "== Cai Docker Engine (get.docker.com)"
  curl -fsSL https://get.docker.com | sudo sh
fi
sudo systemctl enable --now docker ssh 2>/dev/null || sudo systemctl enable --now docker sshd

echo "== Cho phep $(whoami) dung Docker khong can sudo"
sudo usermod -aG docker "$(whoami)"

if command -v ufw >/dev/null 2>&1 && sudo ufw status | grep -q "Status: active"; then
  echo "== Mo cong tuong lua (ufw)"
  for p in 22 3000 8082 3001 9090; do sudo ufw allow "$p/tcp"; done
fi

echo
echo "XONG. Dang xuat roi dang nhap lai de nhom docker co hieu luc, kiem tra: docker run --rm hello-world"
echo "Dia chi IP cua may chu: $(hostname -I | awk '{print $1}')"
