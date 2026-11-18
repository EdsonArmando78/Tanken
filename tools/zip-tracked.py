#!/usr/bin/env python3
"""Comprime archivos rastreados por Git con su contenido actual.

Uso: python tools/zip-tracked.py [salida.zip]
Requiere Python 3 y Git. No modifica el indice ni los archivos del repositorio.
La salida es relativa al directorio actual; por defecto se crea en la raíz del repositorio.
Solo incluye archivos rastreados: no es el paquete final de entrega con documentos
y evidencias sin rastrear. No agrega archivos al índice de Git.
No sobrescribe archivos existentes. Omite archivos eliminados del disco.
Los submodulos no se incluyen. Los enlaces simbolicos se guardan como enlaces.
"""

import argparse
import os
from pathlib import Path
import stat
import subprocess
import sys
import zipfile


def git(root, *args):
    return subprocess.check_output(['git', '-C', str(root), *args])


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('output', nargs='?', type=Path)
    args = parser.parse_args()
    root = Path(os.fsdecode(git(Path(__file__).resolve().parent,
                               'rev-parse', '--show-toplevel')).strip())
    output = (args.output or root / (root.name + '-tracked.zip')).absolute()
    records = git(root, 'ls-files', '--cached', '-z').split(b'\0')
    paths = sorted({os.fsdecode(item) for item in records if item})
    # Validar antes de abrir la salida para evitar comprimirla dentro de si misma.
    if any((root / name).resolve() == output.resolve() for name in paths):
        parser.error('La salida no puede ser un archivo rastreado por Git.')
    count = 0
    skipped = []
    created = False
    try:
        with output.open('xb') as stream:
            created = True
            with zipfile.ZipFile(stream, 'w', zipfile.ZIP_DEFLATED) as archive:
                for name in paths:
                    source = root / name
                    if source.is_symlink():
                        entry = zipfile.ZipInfo(name)
                        entry.create_system = 3
                        entry.external_attr = (stat.S_IFLNK | 0o777) << 16
                        archive.writestr(entry, os.fsencode(os.readlink(source)))
                    elif source.is_file():
                        archive.write(source, arcname=name)
                    else:
                        skipped.append(name)
                        continue
                    count += 1
    except BaseException:
        if created:
            output.unlink(missing_ok=True)
        raise
    print(f'ZIP creado: {output} ({count} archivos)')
    for name in skipped:
        print(f'Omitido (ausente o submodulo): {name}', file=sys.stderr)


if __name__ == '__main__':
    try:
        main()
    except (OSError, subprocess.CalledProcessError, zipfile.BadZipFile) as error:
        print(f'Error: {error}', file=sys.stderr)
        sys.exit(1)
