from functools import reduce
import pandas as pd
import logging
import boto3


class BlockAccounts:
    def __init__(self, auth_tbl_name, customer_tbl_name, customers):
        self.auth_tbl_name = auth_tbl_name
        self.customer_tbl_name = customer_tbl_name
        self.customers = customers
        self.resource = boto3.resource("dynamodb", region_name='ap-southeast-2')
        self._validate()

    def _type_check(self, obj, typ):
        if type(obj) == typ:
            return True
        else:
            msg = f"Object {obj} does not conform to expected type {typ}"
            logging.error(msg)
            return False

    def _validate(self):
        att_types = [
            self._type_check(self.customer_tbl_name, str),
            self._type_check(self.auth_tbl_name, str),
            self._type_check(self.customers, list)
        ]
        valid_types = reduce(lambda first, second: first and second, att_types)
        if not valid_types:
            raise ValueError

    def _get_customer_email(self, table, customerid):
        try:
            response = table.get_item(Key={'customerId': str(customerid)})
            email = response['Item']['emailAddress']
            return email
        except Exception as err:
            print(err)
            logging.error(f"{__name__} Unable to find customerid with xid: {customerid}")

    def get_customer_emails(self, customerids):
        table = self.resource.Table(self.customer_tbl_name)
        for customer in customerids:
            yield self._get_customer_email(table, customer)

    def update_status(self, table, email):
        try:
            response = table.update_item(
                Key={'emailAddress': email},
                UpdateExpression='SET #attr1 = :val1',
                ExpressionAttributeNames={'#attr1': 'status'},
                ExpressionAttributeValues={':val1': 'blocked'},
                ReturnValues="UPDATED_NEW"
            )
            logging.info(response)
            return response
        except Exception as err:
            logging.error(f"Unable to find email: {email}")
            logging.error(err)

    def _close_accounts(self, emails):
        table = self.resource.Table(self.auth_tbl_name)
        for email in emails:
            resp = self.update_status(table=table, email=email)
            yield resp

    @classmethod
    def close_accounts(cls, auth_tbl_name, customer_tbl_name, customers):
        block_acc = cls(auth_tbl_name, customer_tbl_name, customers)
        emails = list(block_acc.get_customer_emails(customers))
        return list(block_acc._close_accounts(emails))


def read_closed_accounts():
    df = pd.read_csv("~/Downloads/closeaccounts.csv", header=None, dtype={0: 'int'})
    xids = list(df[0])
    return xids


def main():
    customers = read_closed_accounts()
    env = "prod"
    auth_table_name = f"{env}.auth.authority"
    customer_table_name = f"{env}.customer-view.customers"

    b_accs = BlockAccounts.close_accounts(
        auth_tbl_name=auth_table_name,
        customer_tbl_name=customer_table_name,
        customers=customers
    )
    return b_accs


if __name__ == "__main__":
    data = main()
    print(data)