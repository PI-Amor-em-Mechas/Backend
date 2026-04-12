import logging
import sys
from pathlib import Path

_PROJECT_ROOT = Path(__file__).resolve().parent.parent
_PROJECT_PARENT = _PROJECT_ROOT.parent
if str(_PROJECT_PARENT) not in sys.path:
    sys.path.insert(0, str(_PROJECT_PARENT))

import reconhecimento_facial.src.config as config
import reconhecimento_facial.src.db as db
from reconhecimento_facial.src.capture import prompt_and_capture
from reconhecimento_facial.src.recognize import run_recognition_loop
from reconhecimento_facial.src.train import train_model


def _configure_logging() -> None:
    level = getattr(logging, config.LOG_LEVEL.upper(), logging.INFO)
    logging.basicConfig(
        level=level,
        format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    )


def _model_trained() -> bool:
    return config.LBPH_MODEL_PATH.exists() and config.LABELS_PATH.exists()


def _print_menu() -> None:
    trained = _model_trained()
    model_status = "[modelo OK]" if trained else "[sem modelo - treinar primeiro]"
    print("\n=== Face Attendance (MediaPipe + LBPH) ===")
    print("1) Cadastrar colaborador e capturar amostras")
    print(f"2) Treinar modelo LBPH")
    print(f"3) Reconhecer e bater ponto  {model_status}")
    print("4) Listar colaboradores")
    print("5) Listar registros de ponto")
    print("0) Sair")


def _list_employees() -> None:
    employees = db.list_employees()
    if not employees:
        print("Nenhum colaborador cadastrado.")
        return

    print("\nColaboradores:")
    for e in employees:
        print(f"- id={e['id']} | nome={e['name']} | criado_em={e['created_at']}")


def _list_punches() -> None:
    raw_limit = input("Limite de registros [50]: ").strip()
    limit = 50
    if raw_limit:
        try:
            limit = int(raw_limit)
        except ValueError:
            print("Valor invalido. Usando 50.")

    punches = db.list_punches(limit=limit)
    if not punches:
        print("Nenhum registro encontrado.")
        return

    print("\nRegistros:")
    for p in punches:
        print(
            f"- id={p['id']} | emp={p['employee_id']} ({p.get('name') or 'sem_nome'}) "
            f"| ts={p['ts']} | tipo={p['type']} | conf={p['confidence']:.2f} "
            f"| img={p.get('image_path') or '-'}"
        )


def main() -> None:
    _configure_logging()
    config.ensure_directories()
    db.init_db()

    while True:
        _print_menu()
        try:
            choice = input("Escolha: ").strip()
        except EOFError:
            print("Entrada encerrada. Saindo.")
            break
        except KeyboardInterrupt:
            print("\nInterrompido pelo usuario. Saindo.")
            break

        try:
            if choice == "1":
                prompt_and_capture()
            elif choice == "2":
                train_model()
                print("Treino concluido.")
            elif choice == "3":
                if not _model_trained():
                    print("Modelo nao encontrado. E necessario treinar antes de reconhecer.")
                    try:
                        resp = input("Deseja treinar agora? [s/N]: ").strip().lower()
                    except (EOFError, KeyboardInterrupt):
                        resp = ""
                    if resp == "s":
                        train_model()
                        print("Treino concluido.")
                    else:
                        print("Reconhecimento cancelado. Execute a opcao 2 primeiro.")
                        continue
                run_recognition_loop()
            elif choice == "4":
                _list_employees()
            elif choice == "5":
                _list_punches()
            elif choice == "0":
                print("Encerrando.")
                break
            else:
                print("Opcao invalida.")
        except Exception as exc:
            logging.exception("Falha ao executar opcao '%s'", choice)
            print(f"Erro ao executar opcao: {exc}")


if __name__ == "__main__":
    main()
