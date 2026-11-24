import importlib.util
import json
from pathlib import Path
import unittest
from unittest.mock import Mock

ROOT = Path(__file__).resolve().parents[2]

def load(path):
    spec = importlib.util.spec_from_file_location("module", ROOT / path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod

bridge = load("cloud/bridge/bridge.py")
receipt = load("serverless/orientation_receipt.py")

class EventsTest(unittest.TestCase):
    def test_bridge_elimina_datos_personales(self):
        event = bridge.to_cloud_event(json.dumps({"event": "orientacion.confirmada",
            "solicitudId": 42, "estudianteEmail": "no-export@example.test"}))
        self.assertEqual(event, {"event": "orientacion.confirmada",
            "eventId": "orientation.confirmed:42", "solicitudId": 42})

    def test_bridge_rechaza_evento_invalido(self):
        with self.assertRaises(ValueError):
            bridge.to_cloud_event('{"event":"orientacion.confirmada","solicitudId":-1}')

    def test_lambda_registra_constancia(self):
        table = Mock()
        body = bridge.to_cloud_event('{"event":"orientacion.confirmada","solicitudId":42}')
        receipt.process({"body": json.dumps(body)}, table)
        self.assertEqual(table.put_item.call_args.kwargs["Item"]["estado"], "CONSTANCIA_GENERADA")
        self.assertEqual(table.put_item.call_args.kwargs["ConditionExpression"],
                         "attribute_not_exists(eventId)")

    def test_lambda_rechaza_id_inconsistente(self):
        table = Mock()
        with self.assertRaises(ValueError):
            receipt.process({"body": '{"event":"orientacion.confirmada","solicitudId":1,"eventId":"x"}'}, table)
        table.put_item.assert_not_called()

    def test_lambda_reentrega_idempotente(self):
        class Duplicate(Exception):
            response = {"Error": {"Code": "ConditionalCheckFailedException"}}
        table = Mock()
        table.put_item.side_effect = Duplicate()
        body = bridge.to_cloud_event('{"event":"orientacion.confirmada","solicitudId":42}')
        receipt.process({"body": json.dumps(body)}, table)

if __name__ == "__main__":
    unittest.main()
