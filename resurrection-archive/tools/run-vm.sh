#!/bin/zsh
export PATH="/opt/homebrew/bin:$PATH"
cd "$VMDIR"
exec qemu-system-i386 -M pc -cpu max -m 1024 \
  -drive file=xp.qcow2,if=ide,index=0,media=disk,format=qcow2 \
  -drive file=install-cd.iso,if=ide,index=1,media=cdrom \
  -usb -device usb-tablet \
  -vga std -netdev user,id=n0 -device rtl8139,netdev=n0 \
  -rtc base=localtime -boot c \
  -monitor unix:/tmp/qemu-mon.sock,server,nowait -vnc 127.0.0.1:1 -name "Cannonballs XP"
