import React, { Component } from 'react';
import axios from 'axios';
import cookie from 'react-cookies';

const headers = {
    'Content-Type': 'application/json',
    'Authorization' : "Bearer "+ cookie.load('access-token')
};


class ManageItem extends Component {
    constructor(props) {
        super(props)

        this.state = {
                 
        }
    }


    deleteUser = (e) => {
        const data = {
            username: this.props.database.username
        }
        axios.post("http://localhost:8080/admin/deleteuser", data, {
            headers: headers
        }).then(res => {
            console.log(res);
            window.location.reload();
        }
        ).catch(e => {
            console.log(e);
        })
    }
    render() {
        const {username, role, email} = this.props.database;
        return ( 
            <div className="user">
                username: {username}, email: {email}, role:  {role} <button onClick={this.deleteUser}>삭제하기</button>
            </div>
        );
    }
}

export default ManageItem
