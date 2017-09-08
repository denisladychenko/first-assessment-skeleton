import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server
let dt = new Date();                 //date object
let utcDate = dt.toUTCString();       //date to string

cli
  .delimiter(cli.chalk['yellow']('ftd~$ command is required...'))
  

cli
  .mode('connect <username> <host> <port>')          //connect takes 3 args username, host and port
  
  .delimiter(cli.chalk['green'](` connected>`))
  .init(function (args, callback) {
    
    username = args.username
    server = connect({ host: args.host, port: args.port }, () => {
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      callback()
    })

    server.on('data', (buffer) => {
      this.log(Message.fromJSON(buffer).toString())
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input, callback) {
    
    const [ command, ...rest ] = words(input)
    
    const contents = rest.join(' ')
    
    if (command === 'disconnect') {
      server.end(new Message({ username, command }).toJSON() + '\n')
      //adds username command
    } else if (command === '@username'){
      //delimiter for username mode
      this.delimiter(cli.chalk['yellow'](`<${utcDate}> <${username}> (whisper):`))
    } else if (command === 'echo') {
      this.delimiter(cli.chalk['pink'](`<${utcDate}> <${username}> (echo):`))
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
      //adds broadcast command
    } else if (command === 'broadcast'){
     
      this.delimiter(cli.chalk['white'](`<${utcDate}> <${username}> (all):`))
     
     server.write(new Message({ username, command, contents }).toJSON() + '\n')
      
      
      //adds users command
    } else if (command === 'users'){
      this.delimiter(cli.chalk['blue'](`<${utcDate}> :currently connected users:`))
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else {
      this.log(`Command <${command}> was not recognized`)
    }
  
    callback()
  })