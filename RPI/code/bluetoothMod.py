#!/usr/bin/python
# -*- coding: utf-8 -*-
from bluetooth import *
from colors import *


class bluetoothComm(object):

	def __init__(self):
		print ''
		self.server_socket = None
		self.client_socket = None
		self.bt_is_connected = False
		
	def init_bluetooth_comm(self):
		while True:
			btPort = 4
			retry = False
			try:
				self.server_sock = BluetoothSocket(RFCOMM)
				self.server_sock.bind(('', btPort))
				self.server_sock.listen(1)
				self.port = self.server_sock.getsockname()[1]
				uuid = '00001101-0000-1000-8000-00805f9b34fb'
				advertise_service(self.server_sock, 'MDP-Server',
								  service_id=uuid, service_classes=[uuid,
								  SERIAL_PORT_CLASS],
								  profiles=[SERIAL_PORT_PROFILE])
				print 'Waiting for bluetooth connection on RFCOMM channel %d..' \
					% self.port
				(self.client_sock, client_info) = self.server_sock.accept()
				cprint(BOLD + GREEN, 'Accepted bluetooth connection from ' + str(client_info))
				self.bt_is_connected = True
				retry = False
			except Exception, e:
				print 'BT-Conn Error: %s ' % str(e)
				retry = True
			if (not retry):
				break
			print 'Retrying Bluetooth connection..'

	def bluetooth_is_connected(self):
		return self.bt_is_connected

	def read_from_bluetooth(self):
		try:
			data = self.client_sock.recv(2048)
			print 'Transmission from Bluetooth: ' + data.rstrip()
			return data
		except BluetoothError, e:
			print 'bluetoothMod/BT-Recv Error: ' + str(e)
			if ('Connection reset by peer' in str(e)):
				self.disconnect_bluetooth()
				cprint(BOLD + RED, 'Assuming Bluetooth disconnected, trying to reconnect..')
				self.init_bluetooth_comm()

	def write_to_bluetooth(self, message):
		try:
			if (not self.bt_is_connected):
				cprint(BOLD + RED, 'Bluetooth not connected! Unable to transmit.')
				return
			self.client_sock.send(str(message))
			#print 'Transmitted to Bluetooth: %s \r\n' % message
		except BluetoothError:
			print 'BT-Send Error: ' + str(e)

	def disconnect_bluetooth(self):
		try:
			if not (self.client_sock is None):
				self.client_socket.close()
				print 'Closing bluetooth client socket..'

			if not (self.server_socket is None):
				self.server_socket.close()
				print 'Closing bluetooth server socket..'
		except Exception, e:
			pass

		self.bt_is_connected = False
