import socket
import sys
import time
from colors import *


class pcComm(object):

	def __init__(self):
		self.ip_address = '192.168.9.9'
		self.port = 1273
		self.pc_is_connect = False

	def close_pc_socket(self):
		if self.conn:
			self.conn.close()
			print 'Terminating server socket..'

		if self.client:
			self.client.close()
			print 'Terminating client socket..'
		
		self.pc_is_connect = False

	def pc_is_connected(self):
		return self.pc_is_connect

	def init_pc_comm(self):
		while True:
			retry = False
			try:
				self.conn = socket.socket(socket.AF_INET,
						socket.SOCK_STREAM)
				self.conn.bind((self.ip_address, self.port))
				self.conn.listen(1)
				print 'Listening for incoming PC connection on ' \
					+ self.ip_address + ':' + str(self.port) + '..'
				(self.client, self.addr) = self.conn.accept()
				cprint(BOLD + GREEN, 'PC connected! IP Address: ' + str(self.addr))
				self.pc_is_connect = True
				retry = False
			except Exception, e:
				print 'PC-Conn Error: %s' % str(e)
				retry = True
			if (not retry):
				break
			print 'Retrying PC connection..'
			time.sleep(1)

	def read_from_PC(self):
		try:
			pc_data = self.client.recv(2048)
			pc_data = pc_data.decode('utf-8')
			if (not pc_data):
				self.close_pc_socket()
				cprint(BOLD + RED, 'Null transmission from PC; Assuming PC disconnected, trying to reconnect..')
				self.init_pc_comm()
				return pc_data
			print 'Transmission from PC: ' + pc_data.rstrip()
			return pc_data
		except Exception, e:
			print 'pcMod/PC-Recv Error: %s' % str(e)
			if ('Broken pipe' in str(e) or 'Connection reset by peer' in str(e)):
				self.close_pc_socket()
				cprint(BOLD + RED, 'PC Broken pipe; Assuming PC disconnected, trying to reconnect..')
				self.init_pc_comm()

	def write_to_PC(self, message):
		try:
			if (not self.pc_is_connect):
				cprint(BOLD + RED, 'PC not connected! Unable to transmit.')
				return
			message = message + '\n'
			self.client.sendto(message, self.addr)
			#print 'Transmitted to PC: ' + message
		except Exception, e:
			print 'pcMod/PC-Send Error: %s' % str(e)
