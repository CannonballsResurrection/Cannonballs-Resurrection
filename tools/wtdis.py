import sys, pefile
from capstone import *

PATH = sys.argv[1]
pe = pefile.PE(PATH)
image_base = pe.OPTIONAL_HEADER.ImageBase

# Build IAT map: address -> imported symbol name
iat = {}
if hasattr(pe, 'DIRECTORY_ENTRY_IMPORT'):
    for entry in pe.DIRECTORY_ENTRY_IMPORT:
        dll = entry.dll.decode(errors='replace')
        for imp in entry.imports:
            name = imp.name.decode(errors='replace') if imp.name else ('ord_%d'%imp.ordinal)
            if imp.address is not None:
                iat[imp.address] = "%s!%s" % (dll, name)

# Build export map: rva -> name
exp = {}
if hasattr(pe, 'DIRECTORY_ENTRY_EXPORT'):
    for e in pe.DIRECTORY_ENTRY_EXPORT.symbols:
        if e.name:
            exp[e.address] = e.name.decode(errors='replace')

def rva_to_off(rva):
    for s in pe.sections:
        if s.VirtualAddress <= rva < s.VirtualAddress + max(s.Misc_VirtualSize, s.SizeOfRawData):
            return s.PointerToRawData + (rva - s.VirtualAddress)
    return None

def read_at_rva(rva, n):
    off = rva_to_off(rva)
    with open(PATH,'rb') as f:
        f.seek(off); return f.read(n)

def disasm(rva, n=200, count=None):
    data = read_at_rva(rva, n)
    md = Cs(CS_ARCH_X86, CS_MODE_32)
    md.detail = True
    va = image_base + rva
    out = []
    i = 0
    for insn in md.disasm(data, va):
        annot = ""
        # annotate call/jmp dword [imm] via IAT
        if insn.mnemonic in ('call','jmp') and '[0x' in insn.op_str:
            try:
                addr = int(insn.op_str.split('[')[1].split(']')[0],16)
                if addr in iat:
                    annot = "  ; -> " + iat[addr]
            except: pass
        # direct call to export
        if insn.mnemonic in ('call','jmp') and insn.op_str.startswith('0x'):
            try:
                tgt = int(insn.op_str,16)
                trva = tgt - image_base
                if trva in exp:
                    annot = "  ; -> " + exp[trva]
            except: pass
        out.append("%08x (rva %05x):  %-10s %s%s" % (insn.address, insn.address-image_base, insn.mnemonic, insn.op_str, annot))
        i += 1
        if count and i>=count: break
    return "\n".join(out)

if __name__ == '__main__':
    cmd = sys.argv[2]
    if cmd == 'exp':
        for rva,name in sorted(exp.items()):
            print("%05x  %s" % (rva, name))
    elif cmd == 'expfind':
        pat = sys.argv[3].lower()
        for rva,name in sorted(exp.items()):
            if pat in name.lower():
                print("%05x  %s" % (rva, name))
    elif cmd == 'dis':
        rva = int(sys.argv[3],16)
        n = int(sys.argv[4]) if len(sys.argv)>4 else 400
        print(disasm(rva, n))
    elif cmd == 'iat':
        for a,n in sorted(iat.items()):
            print("%08x  %s" % (a,n))
