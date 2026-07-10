#!/usr/bin/env python3
import os, sys, pefile, capstone
# DLL to disassemble: CLI arg, else $CB_DLL, else actorobject.dll in the cwd
# (bring your own recovered engine DLL — see source/engine/native/dlls/PROVENANCE.md).
DLL = sys.argv[1] if len(sys.argv) > 1 else os.environ.get('CB_DLL', 'actorobject.dll')
pe = pefile.PE(DLL)
base = pe.OPTIONAL_HEADER.ImageBase
md = capstone.Cs(capstone.CS_ARCH_X86, capstone.CS_MODE_32)

def rva_to_data(rva, n):
    for s in pe.sections:
        if s.VirtualAddress <= rva < s.VirtualAddress + max(s.Misc_VirtualSize, s.SizeOfRawData):
            off = rva - s.VirtualAddress + s.PointerToRawData
            return pe.__data__[off:off+n]
    return b''

def disasm(va, n, label=""):
    if va > 0x1000000: rva = va - base
    else: rva = va; va = base+rva
    data = rva_to_data(rva, n)
    print("==== %s @ VA 0x%x (RVA 0x%x) ====" % (label, va, rva))
    for insn in md.disasm(bytes(data), va):
        print("0x%08x  %-18s %s %s" % (insn.address, insn.bytes.hex(), insn.mnemonic, insn.op_str))

if __name__ == '__main__':
    va = int(sys.argv[1], 0)
    n = int(sys.argv[2]) if len(sys.argv) > 2 else 1200
    label = sys.argv[3] if len(sys.argv) > 3 else ""
    disasm(va, n, label)
