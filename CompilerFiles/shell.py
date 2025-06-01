import sys

import basic

if len(sys.argv) == 1:
	# Access the arguments passed from another script
	# file = str(sys.argv[1:]).removeprefix("['").removesuffix("']")
	file = 'l.txt'
	text = ''
	with open(file, encoding="utf-8") as f:
		text = f.read()
	# result, error = basic.run('<stdin>', f'RUN("{file}")')
	result, error = basic.run('<stdin>', text)
 

	if error:
		print(error.as_string())
	elif result:
		if len(result.elements) == 1:
			print(repr(result.elements[0]))
		else:
			print(repr(result))
else:
	while True:
		text = input('basic > ')
		if text.strip() == "": continue
		result, error = basic.run('<stdin>', text)

		if error:
			print(error.as_string())
		elif result:
			if len(result.elements) == 1:
				print(repr(result.elements[0]))
			else:
				print(repr(result))