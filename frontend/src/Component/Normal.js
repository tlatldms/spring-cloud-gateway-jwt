import React, { Component } from 'react'
import axios from 'axios';
import cookie from 'react-cookies';

class Normal extends Component {
    constructor(props) {
        super(props)

        this.state = {
            isNormal: false,
            accessToken: cookie.load('access-token'),
            refreshToken: cookie.load('refresh-token')
        }
    }

    logout = () => {
        axios.post("http://localhost:8080/newuser/out", {
            accessToken: this.state.accessToken,
        }).then(res => {
            console.log(res);
            window.location.reload();
        }).catch(e => {
            console.log(e);
        }) 
    }

    requestAccessToken = () => {
        axios.post("http://localhost:8080/newuser/refresh", {
            accessToken: this.state.accessToken,
            refreshToken: this.state.refreshToken,
        }).then(res => {
            if (res.data.success) {
                //console.log("success to refresh token to: " + res.data.accessToken);
                cookie.save('access-token', res.data.accessToken, { path: '/' })
                this.setState({
                    isNormal: true
                })
            } else {
                console.log("failed to refresh access token. You need re-login.");
                this.setState({
                    isNormal: false
                })
            }
        }
        ).catch(e => {
            console.log(e);
        })
    }
    componentDidMount(){
        axios.get("http://localhost:8080/user/normal",{
            headers: {
                "Authorization" : "Bearer "+ this.state.accessToken
            }
        }).then(res => {
            if (res.status == 200) {
                this.setState({
                    isNormal:true,
                });
            } 
        }).catch(e => {
            if (e.response) {
                if (e.response.status==500) {
                    this.setState({
                        isNormal: false
                    });
                } else if (e.response.status == 401) {
                    console.clear();
                    this.setState({
                        isNormal: false
                    });
                    this.requestAccessToken();
                }
            }
        })
    }
    render() {
        return (
            <div>
                admin, 일반 user 모두 보이는 페이지.
                로그인한 유저만 보이는 logout button: {this.state.isNormal ? <button onClick={this.logout}> 로그아웃</button> : "" }
            </div>
        )
    }
}

export default Normal
